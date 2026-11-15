
import { isTokenExpired, logout } from "../utils/auth";

const API_BASE_URL = `${
  import.meta.env.VITE_API_BASE_URL || "http://localhost:8080"
}/api`;

export async function apiRequest(endpoint, options = {}) {
  const token = localStorage.getItem("token");

  if (token && isTokenExpired(token)) {
    logout();
    throw new Error("Your session has expired. Please log in again.");
  }

  const headers = {
    "Content-Type": "application/json",
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
    ...options.headers,
  };

  try {
    const response = await fetch(`${API_BASE_URL}${endpoint}`, {
      ...options,
      headers,
    });

    const contentType = response.headers.get("content-type");
    let data = null;

    if (contentType && contentType.includes("json")) {
      data = await response.json();
    } else {
      const text = await response.text();
      data = text ? { message: text } : null;
    }

    if (response.status === 401) {
      logout();
      throw new Error("Your session has expired. Please log in again.");
    }

    if (!response.ok) {
      throw new Error(
        data?.message ||
        data?.detail ||
        data?.error ||
        `Request failed with status ${response.status}.`
      );
    }

    return data;
  } catch (error) {
    console.error("API Request Error:", error);

    if (error instanceof TypeError) {
      throw new Error(
        "Unable to connect to the server. Please check your connection."
      );
    }

    throw error;
  }
}
