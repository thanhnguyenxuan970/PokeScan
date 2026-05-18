import CoreImage
import Foundation
import UIKit

final class PHashService {
    static let shared = PHashService()

    private var setHashes: [String: UInt64] = [:]
    private let ciContext = CIContext()

    private init() {
        loadHashes()
    }

    private func loadHashes() {
        guard let url = Bundle.main.url(forResource: "set_phashes", withExtension: "json"),
              let data = try? Data(contentsOf: url),
              let json = try? JSONSerialization.jsonObject(with: data) as? [String: String]
        else { return }
        for (key, hex) in json {
            if let value = UInt64(hex, radix: 16) {
                setHashes[key] = value
            }
        }
    }

    func computeHash(from pixelBuffer: CVPixelBuffer) -> UInt64? {
        let ciImage = CIImage(cvPixelBuffer: pixelBuffer)
        guard let cgImage = ciContext.createCGImage(ciImage, from: ciImage.extent) else { return nil }
        return computeHash(from: UIImage(cgImage: cgImage))
    }

    func computeHash(from image: UIImage) -> UInt64? {
        // Scale to 32x32 grayscale
        let size = CGSize(width: 32, height: 32)
        let renderer = UIGraphicsImageRenderer(size: size)
        let scaled = renderer.image { ctx in
            ctx.cgContext.setFillColor(UIColor.black.cgColor)
            ctx.cgContext.fill(CGRect(origin: .zero, size: size))
            image.draw(in: CGRect(origin: .zero, size: size))
        }

        guard let cgImage = scaled.cgImage else { return nil }
        let width = 32, height = 32
        var pixelData = [UInt8](repeating: 0, count: width * height)
        let colorSpace = CGColorSpaceCreateDeviceGray()
        guard let context = CGContext(
            data: &pixelData,
            width: width, height: height,
            bitsPerComponent: 8, bytesPerRow: width,
            space: colorSpace,
            bitmapInfo: CGImageAlphaInfo.none.rawValue
        ) else { return nil }
        context.draw(cgImage, in: CGRect(x: 0, y: 0, width: width, height: height))
        let normalised = pixelData.map { Float($0) / 255.0 }

        // 8x8 DCT over 32x32 block
        var dct = [[Float]](repeating: [Float](repeating: 0, count: 8), count: 8)
        let piOver64 = Float.pi / 64.0
        for u in 0..<8 {
            for v in 0..<8 {
                var sum: Float = 0
                for x in 0..<32 {
                    for y in 0..<32 {
                        sum += normalised[y * 32 + x]
                            * cos((Float(2 * x + 1) * Float(u)) * piOver64)
                            * cos((Float(2 * y + 1) * Float(v)) * piOver64)
                    }
                }
                let cu: Float = u == 0 ? 1.0 / sqrt(2.0) : 1.0
                let cv: Float = v == 0 ? 1.0 / sqrt(2.0) : 1.0
                dct[u][v] = sum * cu * cv / 4.0
            }
        }

        // Mean of 63 AC coefficients (exclude DC [0,0])
        var acSum: Float = 0
        for u in 0..<8 { for v in 0..<8 { if u != 0 || v != 0 { acSum += dct[u][v] } } }
        let mean = acSum / 63.0

        // Build 64-bit hash
        var hash: UInt64 = 0
        var bit = 0
        for u in 0..<8 {
            for v in 0..<8 {
                if dct[u][v] > mean { hash |= (1 as UInt64) << bit }
                bit += 1
            }
        }
        return hash
    }

    func hammingDistance(_ a: UInt64, _ b: UInt64) -> Int {
        (a ^ b).nonzeroBitCount
    }

    /// Returns best-matching setCode from candidates, or nil if no match within threshold.
    func findBestMatch(hash: UInt64, candidates: [String]) -> String? {
        var bestCode: String?
        var bestDist = Int.max
        for code in candidates {
            guard let candidateHash = setHashes[code] else { continue }
            let dist = hammingDistance(hash, candidateHash)
            if dist < bestDist {
                bestDist = dist
                bestCode = code
            }
        }
        return bestDist <= 10 ? bestCode : nil
    }
}
