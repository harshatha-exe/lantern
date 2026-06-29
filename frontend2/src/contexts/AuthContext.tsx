import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from "react";
import { authApi } from "@/api/auth";
import { getStoredToken, setStoredToken, setUnauthorizedHandler } from "@/api/client";
import type { User } from "@/types";

interface AuthContextValue {
  token: string | null;
  user: User | null;
  isAuthenticated: boolean;
  isReady: boolean;
  login: (email: string, password: string) => Promise<void>;
  register: (email: string, password: string) => Promise<void>;
  logout: () => void;
}

const AuthContext = createContext<AuthContextValue | null>(null);

function decodeJwtEmail(token: string): string | null {
  try {
    const payload = token.split(".")[1];
    if (!payload) return null;
    const json = JSON.parse(atob(payload.replace(/-/g, "+").replace(/_/g, "/")));
    return json.email ?? json.sub ?? null;
  } catch {
    return null;
  }
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [token, setToken] = useState<string | null>(null);
  const [user, setUser] = useState<User | null>(null);
  const [isReady, setIsReady] = useState(false);

  useEffect(() => {
    const stored = getStoredToken();
    if (stored) {
      setToken(stored);
      const email = decodeJwtEmail(stored);
      if (email) setUser({ email });
    }
    setIsReady(true);
  }, []);

  const applyToken = useCallback((newToken: string) => {
    setStoredToken(newToken);
    setToken(newToken);
    const email = decodeJwtEmail(newToken);
    if (email) setUser({ email });
  }, []);

  const login = useCallback(async (email: string, password: string) => {
    const res = await authApi.login({ email, password });
    if (!res.token) throw new Error("No token returned");
    applyToken(res.token);
    if (!decodeJwtEmail(res.token)) setUser({ email });
  }, [applyToken]);

  const register = useCallback(async (email: string, password: string) => {
    const res = await authApi.register({ email, password });
    if (!res.token) throw new Error("No token returned");
    applyToken(res.token);
    if (!decodeJwtEmail(res.token)) setUser({ email });
  }, [applyToken]);

  const logout = useCallback(() => {
    setStoredToken(null);
    setToken(null);
    setUser(null);
    if (typeof window !== "undefined") window.location.href = "/";
  }, []);

  useEffect(() => {
    setUnauthorizedHandler(() => {
      setToken(null);
      setUser(null);
      if (typeof window !== "undefined" && window.location.pathname !== "/") {
        window.location.href = "/?auth=login";
      }
    });
    return () => setUnauthorizedHandler(null);
  }, []);

  const value = useMemo<AuthContextValue>(() => ({
    token,
    user,
    isAuthenticated: !!token,
    isReady,
    login,
    register,
    logout,
  }), [token, user, isReady, login, register, logout]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used within AuthProvider");
  return ctx;
}
