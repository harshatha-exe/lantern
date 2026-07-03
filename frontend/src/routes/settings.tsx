import { createFileRoute } from "@tanstack/react-router";
import { AppLayout } from "@/layouts/AppLayout";
import { ThemeToggle } from "@/components/ThemeToggle";
import { useTheme } from "@/contexts/ThemeContext";
import { Button } from "@/components/ui/button";
import { useAuth } from "@/contexts/AuthContext";

export const Route = createFileRoute("/settings")({
  ssr: false,
  head: () => ({ meta: [{ title: "Settings — RepoAnalyzer" }] }),
  component: Settings,
});

function Settings() {
  const { theme } = useTheme();
  const { logout } = useAuth();
  return (
    <AppLayout>
      <div className="mx-auto max-w-3xl px-4 sm:px-6 py-8 space-y-6">
        <header>
          <h1 className="text-2xl font-semibold tracking-tight">Settings</h1>
          <p className="text-sm text-muted-foreground">Manage your preferences.</p>
        </header>

        <section className="surface-card p-6 flex items-center justify-between">
          <div>
            <p className="font-medium">Appearance</p>
            <p className="text-sm text-muted-foreground">Current theme: {theme}</p>
          </div>
          <ThemeToggle />
        </section>

        <section className="surface-card p-6 flex items-center justify-between">
          <div>
            <p className="font-medium">Session</p>
            <p className="text-sm text-muted-foreground">Log out of this device.</p>
          </div>
          <Button variant="destructive" onClick={logout}>Logout</Button>
        </section>
      </div>
    </AppLayout>
  );
}
