package com.supfy.app
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.supfy.app.account.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
@RunWith(AndroidJUnit4::class)
class AccountStateTest {
 @Test fun accountDecodeRequiresMultipleUiSignalsAndFiltersEmailNames(){
  assertEquals(AccountStatus.UNKNOWN,AccountController.decode("{\"v\":1,\"status\":2,\"signals\":1,\"name\":\"Example\"}").status)
  assertEquals(AccountSnapshot(AccountStatus.AUTHENTICATED,"Example"),AccountController.decode("{\"v\":1,\"status\":2,\"signals\":2,\"name\":\"Example\"}"))
  assertEquals("",AccountController.decode("{\"v\":1,\"status\":2,\"signals\":2,\"name\":\"private@example.invalid\"}").displayName)
  assertEquals(AccountStatus.GUEST,AccountController.decode("{\"v\":1,\"status\":1,\"signals\":0}").status)
 }
}
