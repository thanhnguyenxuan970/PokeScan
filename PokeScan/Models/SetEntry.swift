import Foundation

struct SetEntry: Codable {
    let setCode: String
    let name: String
    let total: Int
    let printedTotal: Int?  // from pokemontcg.io; used to resolve base1/ex5 collision
    let releaseYear: Int
    let series: String
    let language: String   // "english" | "japanese"
}
