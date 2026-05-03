import Foundation

struct SetEntry: Codable {
    let setCode: String
    let name: String
    let total: Int
    let releaseYear: Int
    let series: String
    let language: String   // "english" | "japanese"
}
