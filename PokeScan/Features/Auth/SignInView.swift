import AuthenticationServices
import SwiftUI

struct SignInView: View {
    @EnvironmentObject var auth: AuthService

    var body: some View {
        VStack(spacing: 24) {
            Image(systemName: "person.crop.circle.badge.checkmark")
                .font(.system(size: 64))
                .foregroundStyle(.secondary)

            Text("Sign in to sync your collection across devices.")
                .multilineTextAlignment(.center)
                .foregroundStyle(.secondary)

            SignInWithAppleButton(.signIn) { request in
                request.requestedScopes = [.email, .fullName]
            } onCompletion: { result in
                if case .success(let authorization) = result {
                    auth.handleAppleAuthorization(authorization)
                }
            }
            .frame(height: 50)
            .signInWithAppleButtonStyle(.black)
        }
        .padding(32)
    }
}
