import Foundation

enum APIError: Error, LocalizedError {
    case invalidURL
    case invalidResponse
    case httpError(Int, String)
    case decodingError(Error)
    case networkError(Error)

    var errorDescription: String? {
        switch self {
        case .invalidURL: return "无效的URL"
        case .invalidResponse: return "无效的服务器响应"
        case .httpError(let code, let msg): return "[\(code)] \(msg)"
        case .decodingError: return "数据解析失败"
        case .networkError(let e): return e.localizedDescription
        }
    }
}

actor APIClient {
    static let shared = APIClient()
    private let baseURL = "https://api.jmi-openatom.cn/api/v1"

    private let decoder: JSONDecoder = JSONDecoder()
    private let encoder: JSONEncoder = JSONEncoder()

    private init() {}

    func request<T: Decodable>(
        method: String = "GET",
        path: String,
        body: (any Encodable)? = nil,
        authenticated: Bool = true
    ) async throws -> T {
        guard var components = URLComponents(string: "\(baseURL)\(path)") else {
            throw APIError.invalidURL
        }

        // For GET requests with body-like params, we don't append query items here
        // The caller should include query params in the path

        guard let url = components.url else {
            throw APIError.invalidURL
        }

        var request = URLRequest(url: url)
        request.httpMethod = method
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.setValue("application/json", forHTTPHeaderField: "Accept")
        request.timeoutInterval = 15

        if authenticated {
            let token = await Session.shared.token
            if let token = token {
                request.setValue(token, forHTTPHeaderField: "jmiopenatom")
                request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
            }
        }

        if let body = body {
            request.httpBody = try encoder.encode(AnyEncodable(body))
        }

        let (data, response): (Data, URLResponse)
        do {
            (data, response) = try await URLSession.shared.data(for: request)
        } catch {
            throw APIError.networkError(error)
        }

        guard let httpResponse = response as? HTTPURLResponse else {
            throw APIError.invalidResponse
        }

        if httpResponse.statusCode == 401 && authenticated {
            await Session.shared.clear()
            throw APIError.httpError(401, "登录已过期，请重新登录")
        }

        // Try to decode the API envelope
        let envelope: ApiResponse<T>
        do {
            envelope = try decoder.decode(ApiResponse<T>.self, from: data)
        } catch {
            // Envelope decode failed, try direct decode
            if let str = String(data: data, encoding: .utf8) {
                throw APIError.httpError(httpResponse.statusCode, str)
            }
            throw APIError.decodingError(error)
        }

        if envelope.code == 0 {
            if let responseData = envelope.data {
                return responseData
            }
            if T.self == EmptyResponse.self {
                return EmptyResponse() as! T
            }
            throw APIError.httpError(envelope.code, envelope.message)
        }
        if envelope.code == 401 && authenticated {
            await Session.shared.clear()
            throw APIError.httpError(401, "登录已过期，请重新登录")
        }
        throw APIError.httpError(envelope.code, envelope.message)
    }

    func get<T: Decodable>(_ path: String, authenticated: Bool = true) async throws -> T {
        try await request(method: "GET", path: path, authenticated: authenticated)
    }

    func post<T: Decodable>(_ path: String, body: (any Encodable)? = nil, authenticated: Bool = true) async throws -> T {
        try await request(method: "POST", path: path, body: body, authenticated: authenticated)
    }

    func patch<T: Decodable>(_ path: String, body: (any Encodable)? = nil, authenticated: Bool = true) async throws -> T {
        try await request(method: "PATCH", path: path, body: body, authenticated: authenticated)
    }

    func delete<T: Decodable>(_ path: String, authenticated: Bool = true) async throws -> T {
        try await request(method: "DELETE", path: path, authenticated: authenticated)
    }
}

struct AnyEncodable: Encodable {
    let value: any Encodable
    init(_ value: any Encodable) { self.value = value }
    func encode(to encoder: Encoder) throws {
        try value.encode(to: encoder)
    }
}
