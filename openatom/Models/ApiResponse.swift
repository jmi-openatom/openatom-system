import Foundation

struct ApiResponse<T: Decodable>: Decodable {
    let code: Int
    let message: String
    let data: T?
    let traceId: String?
}

struct ApiError: Error, LocalizedError {
    let code: Int
    let message: String

    var errorDescription: String? { message }
}

struct EmptyResponse: Decodable {}
