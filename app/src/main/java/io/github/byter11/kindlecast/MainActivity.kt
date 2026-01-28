package io.github.byter11.kindlecast

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.io.File

class MainActivity : ComponentActivity() {

    private val pickFileLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            // handlePickedFile(it)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        var epubUri: Uri? = null
        if (intent?.action == Intent.ACTION_SEND && intent.type == "application/epub+zip") {
            epubUri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
        }

        setContent {
            val colorScheme = if (isSystemInDarkTheme()) dynamicDarkColorScheme(this) else dynamicLightColorScheme(this)
            MaterialTheme(colorScheme = colorScheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                ) {
                    MainScreen(epubUri)
                }
            }
        }
    }
}

@Composable
fun MainScreen(
    epubUri: Uri? = null,
    converter: Converter = viewModel()
) {
    val context = LocalContext.current
    val status by converter.status.collectAsState()
    val logs by converter.logs.collectAsState()

    var server by remember { mutableStateOf<AZW3Server?>(null) }
    val serverIp = remember(server) { server?.getIpAddress() }

    DisposableEffect(Unit) {
        onDispose { server?.stop() }
    }

    LaunchedEffect(epubUri) {
        if (epubUri != null) {
            converter.convertUriToAzw3(context, epubUri)
        }
    }

    LaunchedEffect(status) {
        if (status is ConversionStatus.Success) {
            // Stop any existing instance just in case
            server?.stop()
            server = AZW3Server((status as ConversionStatus.Success).file).apply {
                start()
            }
        } else {
            // If status changes away from Success, ensure server stops
            server?.stop()
            server = null
        }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { converter.convertUriToAzw3(context, it) }
    }

    val saveFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri: Uri? ->
        uri?.let { destinationUri ->
            val success = status as? ConversionStatus.Success
            success?.file?.let { sourceFile ->
                try {
                    context.contentResolver.openOutputStream(destinationUri)?.use { output ->
                        sourceFile.inputStream().use { input ->
                            input.copyTo(output)
                        }
                    }
                } catch (e: Exception) {
                    // Handle IO errors here
                    e.printStackTrace()
                }
            }
        }
    }

    val shareFile: (File) -> Unit = { file ->
        val uri = androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider", // Must match the manifest authority
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/x-mobipocket-ebook"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(intent, "Share Book to Kindle"))
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(40.dp))

            // Main Title
            Text(
                text = "KindleCast",
                fontSize = 48.sp,
                fontWeight = FontWeight.Normal
            )

            Text(
                text = "Version: ${BuildConfig.VERSION_NAME}",
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Description
            Text(
                text = "Convert ebooks and send to your Kindle.",
                fontSize = 18.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            CreditLink("App by", "byter11", "https://github.com/byter11/")
            CreditLink("Uses Calibre for conversion", "calibre-ebook.com", "https://calibre-ebook.com/")

            Spacer(modifier = Modifier.height(8.dp))



            if (logs.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                ConsoleBox(
                    logs = logs,
                    onSaveClick = if (status is ConversionStatus.Success) {
                        {
                            val fileName = (status as ConversionStatus.Success).file.name
                            saveFileLauncher.launch(fileName)
                        }
                    } else null,
                    onShareClick = if (status is ConversionStatus.Success) {
                        {
                            val file = (status as ConversionStatus.Success).file
                            shareFile(file)
                        }
                    } else null,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.height(16.dp))
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }

            Row {
                when (val currentStatus = status) {
                    is ConversionStatus.Idle -> {
                        Button(onClick = { filePickerLauncher.launch("application/epub+zip") }) {
                            Text("SELECT FILE", letterSpacing = 1.sp)
                        }
                    }

                    is ConversionStatus.Success -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Server Active!",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF4CAF50) // Green
                            )
                            Text(
                                text = "On your Kindle, go to:",
                                fontSize = 14.sp
                            )
                            // Display the URL clearly
                            SelectionContainer {
                                Text(
                                    text = "http://$serverIp:8080",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Button(
                                    onClick = {
                                        server?.stop()
                                        server = null
                                        converter.reset() // Assuming you have a reset method to set status to Idle
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.error
                                    )
                                ) {
                                    Text("STOP & CANCEL")
                                }
                            }
                        }
                    }

                    is ConversionStatus.Loading -> {
                        CircularProgressIndicator()
                    }

                    is ConversionStatus.Error -> {
                        // Handle error state
                        Text("Error: ${currentStatus.message}", color = Color.Red)
                        Button(onClick = { converter.reset() }) { Text("RETRY") }
                    }
                }
                }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
fun ConsoleBox(
    logs: String,
    modifier: Modifier = Modifier,
    onSaveClick: (() -> Unit)? = null,
    onShareClick: (() -> Unit)? = null,
) {
    val scrollState = rememberScrollState()

    LaunchedEffect(logs) {
        scrollState.animateScrollTo(scrollState.maxValue)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
    ) {
        // The Console Display
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black, shape = RoundedCornerShape(8.dp))
                .padding(8.dp)
        ) {
            SelectionContainer {
                Text(
                    text = logs,
                    color = Color.White,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    modifier = Modifier.verticalScroll(scrollState)
                )
            }
        }

        // Floating Action Button Container
        if (onSaveClick != null || onShareClick != null) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (onShareClick != null) {
                    SmallFloatingActionButton(
                        onClick = onShareClick,
//                        containerColor = Color.DarkGray,
//                        contentColor = Color.White
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Share")
                    }
                }

                if (onSaveClick != null) {
                    SmallFloatingActionButton(
                        onClick = onSaveClick,
//                        containerColor = Color.DarkGray,
//                        contentColor = Color.White
                    ) {
                        Icon(Icons.Default.Save, contentDescription = "Save")
                    }
                }
            }
        }
    }
}
@Composable
fun CreditLink(mainText: String, linkText: String, url: String) {
    val uriHandler = LocalUriHandler.current

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = mainText,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.width(2.dp))

            Text(
                text = buildAnnotatedString {
                    withLink(
                        link = LinkAnnotation.Url(
                            url = url,
                            linkInteractionListener = {
                                uriHandler.openUri(url)
                            }
                        )
                    ) {
                        append(linkText)
                    }
                },
                fontSize = 12.sp,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    MaterialTheme {
        MainScreen()
    }
}
