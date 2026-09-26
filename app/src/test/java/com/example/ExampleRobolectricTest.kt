package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.util.BarcodeUtil
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("VastraPOS", appName)
    }

    @Test
    fun `barcode util generates valid sku and barcode modules`() {
        val sku = BarcodeUtil.generateSku("Saree", 125)
        assertEquals("SAR-000125", sku)

        val modules = BarcodeUtil.encodeCode128(sku)
        assertTrue(modules.isNotEmpty())
        // Start quiet zone should be 10 white modules
        for (i in 0 until 10) {
            assertEquals(false, modules[i])
        }

        val bitmap = BarcodeUtil.createBarcodeBitmap(sku)
        assertNotNull(bitmap)
        assertEquals(600, bitmap.width)
        assertEquals(220, bitmap.height)
    }
}
