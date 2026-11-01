package com.supfy.app

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.supfy.app.web.*
import android.webkit.ConsoleMessage
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LoginDiagnosticsTest {
    @Test fun documentPresenceDoesNotClaimInteractionThroughAnUnknownOverlay() {
        val payload=JSONObject().put("v",1).put("ready",false).put("contentPresent",true).put("code",0)
            .put("metrics",JSONObject().put("inputs",1).put("hit",0).put("hitOther",2))
        val decoded=LoginProbeSnapshot.decode(JSONObject.quote(payload.toString()))
        assertTrue(decoded.contentPresent);assertFalse(decoded.ready)
        val diagnostics=LoginDiagnostics();diagnostics.observed(decoded)
        val report=diagnostics.report(emptyMap(),"156.0.8078.47")
        assertTrue(report.contains("ready=0 contentPresent=1"))
    }
    @Test fun javascriptResultsAreClassifiedInsteadOfAllBecomingEmpty() {
        assertEquals(LoginProbeSnapshot.Result.NULL_RESULT,LoginProbeSnapshot.decode("null").result)
        assertEquals(LoginProbeSnapshot.Result.MALFORMED,LoginProbeSnapshot.decode("true").result)
        assertEquals(LoginProbeSnapshot.Result.MALFORMED,LoginProbeSnapshot.decode("x".repeat(9000)).result)
        val error=JSONObject().put("v",1).put("ready",true).put("code",1).put("metrics",JSONObject())
        val decoded=LoginProbeSnapshot.decode(JSONObject.quote(error.toString()))
        assertEquals(LoginProbeSnapshot.Result.SCRIPT_EXCEPTION,decoded.result);assertFalse(decoded.ready)
        error.put("code",0.5)
        assertEquals(LoginProbeSnapshot.Result.MALFORMED,LoginProbeSnapshot.decode(error.toString()).result)
    }

    @Test fun onlyBoundedNumericFieldsReachTheReport() {
        val metrics=JSONObject().put("inputs",1).put("width",999999999).put("password","PRIVATE_FIXTURE_VALUE")
            .put("cookie","SESSION_FIXTURE").put("height","EMAIL_FIXTURE").put("origin",1)
        val payload=JSONObject().put("v",1).put("ready",false).put("code",0).put("metrics",metrics)
            .put("token","TOKEN_FIXTURE")
        val decoded=LoginProbeSnapshot.decode(JSONObject.quote(payload.toString()))
        assertEquals(LoginProbeSnapshot.Result.VALID,decoded.result)
        assertEquals(32768,decoded.metrics["width"])
        val diagnostics=LoginDiagnostics();diagnostics.observed(decoded)
        val report=diagnostics.report(mapOf("version" to 1023,"UNEXPECTED_KEY" to 123),"EMAIL_FIXTURE")
        assertTrue(report.contains("webView=UNKNOWN"));assertTrue(report.contains("last.inputs=1"))
        for(value in listOf("PRIVATE_FIXTURE_VALUE","SESSION_FIXTURE","EMAIL_FIXTURE","TOKEN_FIXTURE","UNEXPECTED_KEY","password","cookie"))assertFalse(value,report.contains(value))
    }

    @Test fun resourceDiagnosticsDiscardIdentityQueryAndPath() {
        val diagnostics=LoginDiagnostics()
        diagnostics.failure("https://accounts.spotify.com/login?token=TOKEN_FIXTURE#EMAIL_FIXTURE",LoginDiagnostics.Failure.NETWORK,-2)
        diagnostics.failure("https://accounts.scdn.co/script.js?token=SESSION_FIXTURE",LoginDiagnostics.Failure.SSL,3)
        repeat(25) {diagnostics.failure("https://private.invalid/path/EMAIL_FIXTURE",LoginDiagnostics.Failure.HTTP,400+it)}
        val report=diagnostics.report(emptyMap(),"156.0.8078.47")
        assertTrue(report.contains("ACCOUNTS/NETWORK/-2"));assertTrue(report.contains("ACCOUNT_CDN/SSL/3"))
        assertTrue(report.contains("overflow="))
        for(value in listOf("https://","TOKEN_FIXTURE","EMAIL_FIXTURE","SESSION_FIXTURE","private.invalid","script.js"))assertFalse(value,report.contains(value))
    }

    @Test fun consoleErrorsAreCountedWithoutReadingTheirTextOrSource() {
        val state=WebUiState();state.authenticating=true
        val chrome=SupfyChromeClient(state)
        assertTrue(chrome.onConsoleMessage(ConsoleMessage("PRIVATE_FIXTURE_VALUE","https://private.invalid/?token=TOKEN_FIXTURE",1,ConsoleMessage.MessageLevel.ERROR)))
        val report=state.loginDiagnostics.report(emptyMap(),"156.0.8078.47")
        assertTrue(report.contains("consoleErrors=1"));assertFalse(report.contains("PRIVATE_FIXTURE_VALUE"));assertFalse(report.contains("TOKEN_FIXTURE"))
        chrome.close()
    }
}
