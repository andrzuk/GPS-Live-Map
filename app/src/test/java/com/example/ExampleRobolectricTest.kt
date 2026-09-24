package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.ui.MapViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("GPS Live Map", appName)
    }

    @Test
    fun `mapViewModel initialization`() {
        val application = ApplicationProvider.getApplicationContext<android.app.Application>()
        val viewModel = MapViewModel(application)
        assertNotNull(viewModel.locationState)
    }
}
