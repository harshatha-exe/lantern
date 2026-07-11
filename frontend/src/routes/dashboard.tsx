import { createFileRoute } from "@tanstack/react-router";
import { useEffect, useMemo, useState } from "react";
import { motion } from "framer-motion";
import { Search, Plus, FolderOpen } from "lucide-react";
import { AppLayout } from "@/layouts/AppLayout";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import { ThemeToggle } from "@/components/ThemeToggle";
import { UploadDialog } from "@/components/UploadDialog";
import { RepoCard } from "@/components/RepoCard";
import { CardSkeleton } from "@/components/LoadingSkeleton";
import { AlertDialog, AlertDialogAction, AlertDialogCancel, AlertDialogContent, AlertDialogDescription, AlertDialogFooter, AlertDialogHeader, AlertDialogTitle } from "@/components/ui/alert-dialog";
import { repositoriesApi } from "@/api/repositories";
import { useAuth } from "@/contexts/AuthContext";
import { extractErrorMessage } from "@/api/client";
import { toast } from "sonner";
import type { Repository } from "@/types";

export const Route = createFileRoute("/dashboard")({
  ssr: false,
  head: () => ({ meta: [{ title: "Dashboard — Lantern" }] }),
  component: Dashboard,
});

function Dashboard() {
  const { user } = useAuth();
  const [uploadOpen, setUploadOpen] = useState(false);
  const [query, setQuery] = useState("");
  const [repos, setRepos] = useState<Repository[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [toDelete, setToDelete] = useState<string | null>(null);

  useEffect(() => {
    let mounted = true;
    (async () => {
      try {
        const list = await repositoriesApi.getAll();
        if (mounted) setRepos(list);
      } catch (err) {
        if (mounted) setError(extractErrorMessage(err));
      } finally {
        if (mounted) setLoading(false);
      }
    })();
    return () => { mounted = false; };
  }, []);

  const filtered = useMemo(() => {
    const q = query.trim().toLowerCase();
    const sorted = [...repos].sort((a, b) => {
      const da = new Date(a.createdAt ?? 0).getTime();
      const db = new Date(b.createdAt ?? 0).getTime();
      return db - da;
    });
    if (!q) return sorted;
    return sorted.filter((r) =>
      (r.githubUrl ?? r.id).toLowerCase().includes(q),
    );
  }, [repos, query]);

  async function confirmDelete() {
    if (!toDelete) return;
    try {
      await repositoriesApi.remove(toDelete);
      setRepos((prev) => prev?.filter((r) => r.id !== toDelete) ?? prev);
      toast.success("Repository deleted");
    } catch (err) {
      toast.error(extractErrorMessage(err));
    } finally {
      setToDelete(null);
    }
  }

  const initials = (user?.email ?? "U").slice(0, 2).toUpperCase();

  return (
    <AppLayout>
      <div className="mx-auto max-w-6xl px-4 sm:px-6 py-6">
        {/* Top bar */}
        <div className="flex flex-col sm:flex-row sm:items-center gap-3 mb-8">
          <div className="flex-1">
            <h1 className="text-2xl font-semibold tracking-tight">Repositories</h1>
            <p className="text-sm text-muted-foreground">Browse, upload and analyze your codebases.</p>
          </div>
          <div className="flex items-center gap-2">
            <div className="relative">
              <Search className="absolute left-2.5 top-2.5 h-4 w-4 text-muted-foreground" />
              <Input
                value={query}
                onChange={(e) => setQuery(e.target.value)}
                placeholder="Search repositories"
                className="pl-8 w-full sm:w-64"
              />
            </div>
            <Button onClick={() => setUploadOpen(true)} className="gap-2">
              <Plus className="h-4 w-4" /> Upload
            </Button>
            <ThemeToggle />
            <Avatar className="h-9 w-9">
              <AvatarFallback>{initials}</AvatarFallback>
            </Avatar>
          </div>
        </div>

        {/* Content */}
        {loading ? (
          <div className="grid sm:grid-cols-2 lg:grid-cols-3 gap-4">
            {Array.from({ length: 6 }).map((_, i) => <CardSkeleton key={i} />)}
          </div>
        ) : error ? (
          <EmptyState title="Could not load repositories" description={error} />
        ) : filtered.length === 0 ? (
          <EmptyState
            title={query ? "No matches" : "No repositories yet"}
            description={query ? "Try a different search term." : "Upload a repo to get started."}
            cta={!query ? <Button onClick={() => setUploadOpen(true)}><Plus className="h-4 w-4" /> Upload</Button> : undefined}
          />
        ) : (
          <motion.div layout className="grid sm:grid-cols-2 lg:grid-cols-3 gap-4">
            {filtered.map((repo) => (
              <RepoCard key={repo.id} repo={repo} onDelete={(id) => setToDelete(id)} />
            ))}
          </motion.div>
        )}
      </div>

      <UploadDialog open={uploadOpen} onOpenChange={setUploadOpen} />

      <AlertDialog open={!!toDelete} onOpenChange={(o) => !o && setToDelete(null)}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Delete this repository?</AlertDialogTitle>
            <AlertDialogDescription>
              This action cannot be undone. The repository and its analysis will be removed.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>Cancel</AlertDialogCancel>
            <AlertDialogAction onClick={confirmDelete} className="bg-destructive text-destructive-foreground hover:bg-destructive/90">
              Delete
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </AppLayout>
  );
}

function EmptyState({ title, description, cta }: { title: string; description: string; cta?: React.ReactNode }) {
  return (
    <div className="surface-card flex flex-col items-center justify-center text-center p-12">
      <FolderOpen className="h-10 w-10 text-muted-foreground" />
      <h3 className="mt-4 font-semibold">{title}</h3>
      <p className="mt-1 text-sm text-muted-foreground max-w-md">{description}</p>
      {cta && <div className="mt-5">{cta}</div>}
    </div>
  );
}
