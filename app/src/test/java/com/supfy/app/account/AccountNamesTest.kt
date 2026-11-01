package com.supfy.app.account

import org.junit.Assert.*
import org.junit.Test

class AccountNamesTest {
    @Test fun publicDisplayNameIsBoundedAndSanitized() {
        assertEquals("Deniz Kaya",AccountNames.clean(" Deniz\n Kaya "))
        assertEquals("",AccountNames.clean("x".repeat(81)))
        assertEquals("Deniz",AccountNames.clean("\u202eDeniz\u202c"))
    }
    @Test fun accountSecretsAndGenericLabelsAreNeverShownAsNames() {
        for(value in listOf("person@example.com","Bearer PRIVATE","https://example.com/","Your profile","Profil","Hesap","token=PRIVATE"))assertEquals(value,"",AccountNames.clean(value))
        assertEquals("",AccountNames.clean(null))
    }
}
