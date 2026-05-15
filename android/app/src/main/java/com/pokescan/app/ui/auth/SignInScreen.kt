package com.pokescan.app.ui.auth

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.pokescan.app.R
import com.pokescan.app.config.AppConfig

@Composable
fun SignInScreen(
    onAuthSuccess: () -> Unit,
    onGuestMode: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            viewModel.handleSignInResult(task)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is AuthEvent.NavigateToScanner -> onAuthSuccess()
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFFFAFAFA)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_launcher_foreground),
                    contentDescription = null,
                    modifier = Modifier.size(88.dp),
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Sign in to PokeScan",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black),
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Sync your collection across devices.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(40.dp))

            when (val s = state) {
                is AuthState.Loading -> CircularProgressIndicator(modifier = Modifier.size(48.dp))
                is AuthState.Error -> {
                    Text(
                        text = s.message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    GoogleSignInButton(onClick = {
                        viewModel.clearError()
                        launcher.launch(viewModel.googleSignInClient.signInIntent)
                    })
                }
                else -> {
                    GoogleSignInButton(onClick = {
                        launcher.launch(viewModel.googleSignInClient.signInIntent)
                    })
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = onGuestMode,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = "Continue as Guest",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
            TermsFooter(onOpenUrl = { url ->
                if (url.isNotBlank()) {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                }
            })
        }
    }
}

@Composable
private fun GoogleSignInButton(onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_google),
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = Color.Unspecified,
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = "Continue with Google")
    }
}

@Composable
private fun TermsFooter(onOpenUrl: (String) -> Unit) {
    val linkColor = MaterialTheme.colorScheme.primary
    val baseColor = MaterialTheme.colorScheme.onSurfaceVariant
    val annotated = buildAnnotatedString {
        withStyle(SpanStyle(color = baseColor)) { append("By continuing you agree to our ") }
        pushStringAnnotation(tag = "TOS", annotation = AppConfig.PRIVACY_POLICY_URL) // TODO: replace with ToS URL when available
        withStyle(SpanStyle(color = linkColor)) { append("Terms of Service") }
        pop()
        withStyle(SpanStyle(color = baseColor)) { append(" and ") }
        pushStringAnnotation(tag = "PP", annotation = AppConfig.PRIVACY_POLICY_URL)
        withStyle(SpanStyle(color = linkColor)) { append("Privacy Policy") }
        pop()
        withStyle(SpanStyle(color = baseColor)) { append(".") }
    }
    ClickableText(
        text = annotated,
        style = MaterialTheme.typography.bodySmall.copy(textAlign = TextAlign.Center),
        onClick = { offset ->
            annotated.getStringAnnotations(start = offset, end = offset)
                .firstOrNull()?.let { onOpenUrl(it.item) }
        },
    )
}
