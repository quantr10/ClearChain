package com.clearchain.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.clearchain.app.ui.theme.ClearChainTheme
import okhttp3.*
import java.io.IOException
import android.util.Log

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        testBackendConnection()
        enableEdgeToEdge()
        setContent {
            ClearChainTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        name = "Android",
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    ClearChainTheme {
        Greeting("Android")
    }
}

private fun testBackendConnection() {
    val client = OkHttpClient()

    val request = Request.Builder()
        .url("http://10.0.2.2:5000/")
        .build()

    client.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            println("Backend connection failed: ${e.message}")
        }

        override fun onResponse(call: Call, response: Response) {
            println("Backend connected! ${response.body?.string()}")
        }
    })
}