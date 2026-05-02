import SwiftUI

struct ScannerView: View {
    @StateObject private var viewModel = CameraViewModel()

    private let reticleWidthRatio: CGFloat = 0.85
    private let cardAspectRatio: CGFloat = 2.5 / 3.5

    var body: some View {
        Group {
            if viewModel.isAuthorized {
                ZStack {
                    CameraPreviewView(session: viewModel.session)
                        .ignoresSafeArea()
                        .onAppear { viewModel.startSession() }
                        .onDisappear { viewModel.stopSession() }

                    reticleOverlay

                    VStack {
                        Spacer()
                        scanButton
                            .padding(.bottom, 48)
                    }
                }
            } else {
                ZStack {
                    Color.black.ignoresSafeArea()
                    Text("Camera access required.\nGo to Settings → PokeScan → Camera.")
                        .multilineTextAlignment(.center)
                        .foregroundStyle(.white)
                        .padding()
                }
            }
        }
    }

    // MARK: - Reticle

    private var reticleOverlay: some View {
        GeometryReader { geo in
            let width = geo.size.width * reticleWidthRatio
            let height = width / cardAspectRatio

            ZStack {
                // Dimmed surround — drawingGroup ensures destinationOut composites correctly
                Color.black.opacity(0.45)
                    .ignoresSafeArea()
                    .mask(
                        Rectangle()
                            .ignoresSafeArea()
                            .overlay(
                                RoundedRectangle(cornerRadius: 12)
                                    .frame(width: width, height: height)
                                    .blendMode(.destinationOut)
                            )
                            .drawingGroup()
                    )

                // Border
                RoundedRectangle(cornerRadius: 12)
                    .strokeBorder(reticleBorderColor, lineWidth: 2)
                    .frame(width: width, height: height)
                    .position(x: geo.size.width / 2, y: geo.size.height / 2)
            }
        }
    }

    private var reticleBorderColor: Color {
        switch viewModel.scanState {
        case .idle:     .white.opacity(0.7)
        case .scanning: .yellow
        case .detected: .green
        case .loading:  .blue
        case .result:   .green
        }
    }

    // MARK: - Scan Button

    private var scanButton: some View {
        Button {
            if viewModel.scanState == .result {
                viewModel.resetScan()
            } else {
                viewModel.startScan()
            }
        } label: {
            Text(scanButtonLabel)
                .font(.system(size: 17, weight: .semibold))
                .foregroundStyle(.white)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 16)
                .background(scanButtonBackground)
                .clipShape(Capsule())
                .padding(.horizontal, 40)
        }
        .disabled(viewModel.scanState == .scanning || viewModel.scanState == .loading)
    }

    private var scanButtonLabel: String {
        switch viewModel.scanState {
        case .idle:     "Tap to Scan"
        case .scanning: "Scanning…"
        case .detected: "Card Detected"
        case .loading:  "Fetching Price…"
        case .result:   "Scan Another"
        }
    }

    private var scanButtonBackground: Color {
        switch viewModel.scanState {
        case .idle:             .white.opacity(0.2)
        case .scanning:         .yellow.opacity(0.3)
        case .detected, .result: .green.opacity(0.3)
        case .loading:          .blue.opacity(0.3)
        }
    }
}

#Preview {
    ScannerView()
}
