import { createFileRoute } from "@tanstack/react-router";
import { AppLayout } from "@/layouts/AppLayout";
import { useAuth } from "@/contexts/AuthContext";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";

export const Route = createFileRoute("/profile")({
  ssr: false,
  head: () => ({ meta: [{ title: "Profile — Lantern" }] }),
  component: Profile,
});

function Profile() {
  const { user } = useAuth();
  const initials = (user?.email ?? "U").slice(0, 2).toUpperCase();
  return (
    <AppLayout>
      <div className="mx-auto max-w-3xl px-4 sm:px-6 py-8 space-y-6">
        <header>
          <h1 className="text-2xl font-semibold tracking-tight">Profile</h1>
          <p className="text-sm text-muted-foreground">Your account information.</p>
        </header>
        <div className="surface-card p-6 flex items-center gap-4">
          <Avatar className="h-16 w-16">
            <AvatarFallback className="text-lg">{initials}</AvatarFallback>
          </Avatar>
          <div>
            <p className="font-semibold">{user?.email ?? "Unknown"}</p>
            <p className="text-sm text-muted-foreground">Signed in</p>
          </div>
        </div>
        <p className="text-xs text-muted-foreground">
          More profile details coming soon.
        </p>
      </div>
    </AppLayout>
  );
}
