package com.supfy.app.playback
import org.junit.Assert.*
import org.junit.Test
class PlaybackSnapshotTest {
 @Test fun metadataAndPlaybackTimesAreBounded(){val s=PlaybackSnapshot(title="a".repeat(500),artist="b".repeat(500),positionMs=Long.MAX_VALUE,durationMs=Long.MAX_VALUE).bounded();assertEquals(200,s.title.length);assertEquals(120,s.artist.length);assertEquals(PlaybackSnapshot.MAX_TIME,s.positionMs);assertEquals(PlaybackSnapshot.MAX_TIME,s.durationMs)}
 @Test fun negativeTimesAndPositionsBeyondDurationCannotReachNotification(){val a=PlaybackSnapshot(positionMs=-100,durationMs=-2).bounded();assertEquals(0L,a.positionMs);assertEquals(0L,a.durationMs);val b=PlaybackSnapshot(positionMs=10000,durationMs=1000).bounded();assertEquals(1000L,b.positionMs)}
 @Test fun repeatedPausedSnapshotsCannotExtendTheServiceIdleDeadline(){val lease=PausedPlaybackLease();assertEquals(300000L,lease.remainingMs(false,1000));assertEquals(299000L,lease.remainingMs(false,2000));assertEquals(0L,lease.remainingMs(false,301000));assertEquals(0L,lease.remainingMs(false,400000))}
 @Test fun actualPlaybackResetsThePausedIdleDeadline(){val lease=PausedPlaybackLease();lease.remainingMs(false,1000);assertNull(lease.remainingMs(true,2000));assertEquals(300000L,lease.remainingMs(false,3000))}
}

class PlaybackStartGateTest {
 @Test fun actualPlaybackCanStartDuringARecentVisibleTransitionOnly(){val gate=PlaybackStartGate();gate.setForeground(true,1000);assertTrue(gate.canStart(true,true,1100));gate.setForeground(false,2000);assertTrue(gate.canStart(true,true,4999));assertFalse(gate.canStart(true,true,5001));gate.setForeground(false,6000);assertFalse(gate.canStart(true,true,6001))}
 @Test fun backgroundWithoutAVisibleTransitionCannotStart(){val gate=PlaybackStartGate();gate.setForeground(false,1000);assertFalse(gate.canStart(true,true,1100))}
 @Test fun pausedOrDisabledPlaybackCannotUseTheTransitionGrace(){val gate=PlaybackStartGate();gate.setForeground(true,1000);gate.setForeground(false,2000);assertFalse(gate.canStart(false,true,2100));assertFalse(gate.canStart(true,false,2100))}
}
