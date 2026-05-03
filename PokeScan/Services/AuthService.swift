import AuthenticationServices
import Foundation
import Security

@MainActor
final class AuthService: NSObject, ObservableObject {
    static let shared = AuthService()

    @Published var isSignedIn: Bool = false
    @Published var userID: String? = nil

    var tier: String { StoreKitService.shared.isPro ? "pro" : "free" }

    private override init() {
        super.init()
    }

    func restoreSession() {
        guard let storedID = UserDefaults.standard.string(forKey: KeychainKeys.appleUserID),
              readTokenFromKeychain() != nil
        else { return }

        let provider = ASAuthorizationAppleIDProvider()
        provider.getCredentialState(forUserID: storedID) { [weak self] state, _ in
            Task { @MainActor in
                if state == .authorized {
                    self?.isSignedIn = true
                    self?.userID = storedID
                } else {
                    self?.clearSession()
                }
            }
        }
    }

    func signIn() {
        let provider = ASAuthorizationAppleIDProvider()
        let request = provider.createRequest()
        request.requestedScopes = [.email, .fullName]
        let controller = ASAuthorizationController(authorizationRequests: [request])
        controller.delegate = self
        controller.presentationContextProvider = self
        controller.performRequests()
    }

    func signOut() {
        clearSession()
    }

    func handleAppleAuthorization(_ authorization: ASAuthorization) {
        guard let credential = authorization.credential as? ASAuthorizationAppleIDCredential,
              let identityToken = credential.identityToken
        else { return }
        let appleUserID = credential.user
        Task { @MainActor in
            await self.exchangeForServerToken(identityToken: identityToken, appleUserID: appleUserID)
        }
    }

    // MARK: - Keychain

    func readTokenFromKeychain() -> String? {
        let query: [CFString: Any] = [
            kSecClass: kSecClassGenericPassword,
            kSecAttrAccount: KeychainKeys.serverToken,
            kSecReturnData: true,
            kSecMatchLimit: kSecMatchLimitOne,
        ]
        var result: AnyObject?
        let status = SecItemCopyMatching(query as CFDictionary, &result)
        guard status == errSecSuccess,
              let data = result as? Data,
              let token = String(data: data, encoding: .utf8)
        else { return nil }
        return token
    }

    private func saveTokenToKeychain(_ token: String) {
        let data = Data(token.utf8)
        let query: [CFString: Any] = [
            kSecClass: kSecClassGenericPassword,
            kSecAttrAccount: KeychainKeys.serverToken,
            kSecValueData: data,
            kSecAttrAccessible: kSecAttrAccessibleAfterFirstUnlock,
        ]
        SecItemDelete(query as CFDictionary)
        SecItemAdd(query as CFDictionary, nil)
    }

    private func clearSession() {
        let query: [CFString: Any] = [
            kSecClass: kSecClassGenericPassword,
            kSecAttrAccount: KeychainKeys.serverToken,
        ]
        SecItemDelete(query as CFDictionary)
        UserDefaults.standard.removeObject(forKey: KeychainKeys.appleUserID)
        isSignedIn = false
        userID = nil
    }

    // MARK: - Backend exchange

    private func exchangeForServerToken(identityToken: Data, appleUserID: String) async {
        guard let tokenString = String(data: identityToken, encoding: .utf8) else { return }
        var request = URLRequest(url: AppConfig.backendBaseURL.appendingPathComponent("auth/apple"))
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = try? JSONSerialization.data(withJSONObject: [
            "identity_token": tokenString,
            "apple_user_id": appleUserID,
        ])

        guard let (data, response) = try? await URLSession.shared.data(for: request),
              let http = response as? HTTPURLResponse,
              http.statusCode == 200,
              let json = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
              let serverToken = json["token"] as? String
        else { return }

        saveTokenToKeychain(serverToken)
        UserDefaults.standard.set(appleUserID, forKey: KeychainKeys.appleUserID)
        isSignedIn = true
        userID = appleUserID
    }
}

// MARK: - ASAuthorizationControllerDelegate

extension AuthService: ASAuthorizationControllerDelegate {
    nonisolated func authorizationController(
        controller: ASAuthorizationController,
        didCompleteWithAuthorization authorization: ASAuthorization
    ) {
        guard let credential = authorization.credential as? ASAuthorizationAppleIDCredential,
              let identityToken = credential.identityToken
        else { return }

        let appleUserID = credential.user
        Task { @MainActor in
            await self.exchangeForServerToken(
                identityToken: identityToken,
                appleUserID: appleUserID
            )
        }
    }

    nonisolated func authorizationController(
        controller: ASAuthorizationController,
        didCompleteWithError error: Error
    ) {
        // User cancelled or error — no state change needed.
    }
}

// MARK: - ASAuthorizationControllerPresentationContextProviding

extension AuthService: ASAuthorizationControllerPresentationContextProviding {
    nonisolated func presentationAnchor(for controller: ASAuthorizationController) -> ASPresentationAnchor {
        UIApplication.shared.connectedScenes
            .compactMap { $0 as? UIWindowScene }
            .first?
            .windows
            .first { $0.isKeyWindow } ?? ASPresentationAnchor()
    }
}
