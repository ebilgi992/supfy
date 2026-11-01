package com.supfy.app.web
import org.junit.Assert.*
import org.junit.Test
class AuthRedirectGateTest {
 @Test fun onlyRecentAuthInteractionAllowsOneProviderRedirect() {
  val gate=AuthRedirectGate();assertFalse(gate.consume("https://accounts.google.com/signin",100,false))
  gate.arm(PlayerNavigation.LOGIN_URL,100);assertTrue(gate.consume("https://accounts.google.com/signin",101,false))
  assertFalse(gate.consume("https://accounts.google.com/signin",102,false))
  gate.arm(PlayerNavigation.LOGIN_URL,100);assertFalse(gate.consume("https://appleid.apple.com/auth",15101,false))
 }
 @Test fun untrustedPagesAndLookalikeProvidersNeverInheritAGrant() {
  val gate=AuthRedirectGate();gate.arm("https://evil.invalid/login",100)
  assertFalse(gate.consume("https://accounts.google.com/signin",101,false))
  gate.arm(PlayerNavigation.LOGIN_URL,100)
  for(url in listOf("https://accounts.google.com.evil.invalid/","https://user@accounts.google.com/","http://accounts.google.com/","https://accounts.google.com:444/","javascript:alert(1)"))assertFalse(url,gate.consume(url,101,true))
 }
}
