import { createFileRoute } from "@tanstack/react-router";
import { useEffect, useState } from "react";
import { motion } from "framer-motion";
import {
  Layers, FileText, GitBranch, Code2, Zap, ShieldCheck, CheckCircle2, FlaskConical, Sparkles, ChevronDown,
} from "lucide-react";
import { AppLayout } from "@/layouts/AppLayout";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Collapsible, CollapsibleContent, CollapsibleTrigger } from "@/components/ui/collapsible";
import { LineSkeleton } from "@/components/LoadingSkeleton";
import { ChatSidebar } from "@/components/ChatSidebar";
import { repositoriesApi } from "@/api/repositories";
import { extractErrorMessage } from "@/api/client";
import { toast } from "sonner";
import type { Repository } from "@/types";
import { cn } from "@/lib/utils";

export const Route = createFileRoute("/repository/$id")({
  ssr: false,
  head: () => ({ meta: [{ title: "Repository — RepoAnalyzer" }] }),
  component: RepositoryPage,
});

const FEATURES = [
  { key: "structure", title: "Project Structure", icon: Layers },
  { key: "readme", title: "README Generator", icon: FileText },
  { key: "architecture", title: "Architecture Summary", icon: GitBranch },
  { key: "summary", title: "Code Summary", icon: Code2 },
  { key: "dependencies", title: "Dependency Analysis", icon: Zap },
  { key: "api", title: "API Documentation", icon: FileText },
  { key: "security", title: "Security Analysis", icon: ShieldCheck },
  { key: "quality", title: "Code Quality", icon: CheckCircle2 },
  { key: "tests", title: "Test Coverage", icon: FlaskConical },
] as const;

function RepositoryPage() {
  const { id } = Route.useParams();
  const [repo, setRepo] = useState<Repository | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let mounted = true;
    let timer: ReturnType<typeof setTimeout> | null = null;

    async function poll() {
      try {
        const data = await repositoriesApi.getStatus(id);
        if (!mounted) return;
        setRepo(data);
        setLoading(false);
        const status = String(data.status ?? "").toUpperCase();
        if (status === "PROCESSING" || status === "PENDING") {
          timer = setTimeout(poll, 3000);
        }
      } catch (err) {
        if (!mounted) return;
        setError(extractErrorMessage(err));
        setLoading(false);
      }
    }
    poll();
    return () => {
      mounted = false;
      if (timer) clearTimeout(timer);
    };
  }, [id]);

  const status = String(repo?.status ?? "").toUpperCase();
  const isProcessing = status === "PROCESSING" || status === "PENDING";

  return (
    <AppLayout>
      <div className="mx-auto max-w-5xl px-4 sm:px-6 py-6">
        {/* Header */}
        <motion.div initial={{ opacity: 0, y: 8 }} animate={{ opacity: 1, y: 0 }}>
          <div className="flex items-start justify-between gap-4 flex-wrap">
            <div className="min-w-0">
              <h1 className="text-2xl sm:text-3xl font-semibold tracking-tight truncate">
                {repo?.name ?? repo?.githubUrl ?? id}
              </h1>
              {repo?.githubUrl && (
                <a
                  href={repo.githubUrl}
                  target="_blank"
                  rel="noreferrer"
                  className="text-sm text-muted-foreground hover:text-foreground"
                >
                  {repo.githubUrl}
                </a>
              )}
            </div>
            {repo?.status && (
              <Badge variant={isProcessing ? "secondary" : status === "FAILED" ? "destructive" : "default"}>
                {isProcessing && <span className="mr-1 h-1.5 w-1.5 rounded-full bg-current animate-pulse" />}
                {repo.status}
              </Badge>
            )}
          </div>

          <div className="mt-5">
            <Button className="gap-2" disabled={isProcessing}>
              <Sparkles className="h-4 w-4" /> Generate
            </Button>
          </div>
        </motion.div>

        {/* Status banner */}
        {loading && (
          <div className="surface-card mt-6 p-5">
            <LineSkeleton lines={3} />
          </div>
        )}

        {error && (
          <div className="surface-card mt-6 p-5 border-destructive/40">
            <p className="text-sm text-destructive">{error}</p>
          </div>
        )}

        {isProcessing && !loading && (
          <div className="surface-card mt-6 p-5 flex items-center gap-3">
            <div className="h-2 w-2 rounded-full bg-primary animate-pulse" />
            <p className="text-sm text-muted-foreground">
              Analysis in progress — results will appear automatically.
            </p>
          </div>
        )}

        {/* Features */}
        <div className="mt-6 space-y-3">
          {FEATURES.map((feature, i) => (
            <FeatureCollapsible
              key={feature.key}
              feature={feature}
              repo={repo}
              isProcessing={isProcessing}
              index={i}
            />
          ))}
        </div>
      </div>

      <ChatSidebar repositoryId={id} />
    </AppLayout>
  );
}

function FeatureCollapsible({
  feature, repo, isProcessing, index,
}: {
  feature: { key: string; title: string; icon: typeof Layers };
  repo: Repository | null;
  isProcessing: boolean;
  index: number;
}) {
  const [open, setOpen] = useState(false);
  const Icon = feature.icon;
  // TODO(backend): No endpoint yet for per-feature artifacts. Once available,
  // read cached results from repo[feature.key] or fetch GET /api/v1/repositories/{id}/artifacts/{key}.
  const cached = repo?.[feature.key];

  async function generate() {
    // TODO(backend): No per-feature generation endpoint yet. Replace with a real call.
    toast.info("Per-feature generation endpoint not yet available on the backend.");
  }

  return (
    <motion.div
      initial={{ opacity: 0, y: 6 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ delay: index * 0.03 }}
      className="surface-card overflow-hidden"
    >
      <Collapsible open={open} onOpenChange={setOpen}>
        <CollapsibleTrigger className="flex w-full items-center justify-between gap-4 p-4 text-left hover:bg-muted/40 transition-colors">
          <div className="flex items-center gap-3 min-w-0">
            <div className="flex h-9 w-9 items-center justify-center rounded-lg bg-primary/10 text-primary">
              <Icon className="h-4 w-4" />
            </div>
            <div className="min-w-0">
              <p className="font-medium">{feature.title}</p>
              <p className="text-xs text-muted-foreground">
                {cached ? "Cached result available" : isProcessing ? "Waiting for analysis…" : "Not generated yet"}
              </p>
            </div>
          </div>
          <ChevronDown className={cn("h-4 w-4 text-muted-foreground transition-transform", open && "rotate-180")} />
        </CollapsibleTrigger>
        <CollapsibleContent>
          <div className="border-t border-border p-4 text-sm">
            {isProcessing && !cached ? (
              <LineSkeleton lines={5} />
            ) : cached ? (
              <pre className="whitespace-pre-wrap text-xs bg-muted/40 p-3 rounded-md overflow-auto max-h-96">
                {typeof cached === "string" ? cached : JSON.stringify(cached, null, 2)}
              </pre>
            ) : (
              <div className="flex items-center justify-between gap-3">
                <p className="text-muted-foreground">No result available yet for this feature.</p>
                <Button size="sm" variant="outline" onClick={generate}>Generate</Button>
              </div>
            )}
          </div>
        </CollapsibleContent>
      </Collapsible>
    </motion.div>
  );
}
