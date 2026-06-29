import { type ReactNode } from "react";
import { Sidebar } from "@/components/Sidebar";
import { ProtectedRoute } from "@/components/ProtectedRoute";

export function AppLayout({ children }: { children: ReactNode }) {
  return (
    <ProtectedRoute>
      <div className="flex min-h-screen w-full">
        <Sidebar />
        <main className="flex-1 min-w-0 pt-14 md:pt-0">{children}</main>
      </div>
    </ProtectedRoute>
  );
}
