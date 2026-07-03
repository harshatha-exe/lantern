import { motion } from "framer-motion";
import { Github, Trash2, ExternalLink, Clock, Tags, FileArchive } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Link } from "@tanstack/react-router";
import type { Repository } from "@/types";

interface Props {
  repo: Repository;
  onDelete?: (id: string) => void;
}

function formatDate(value?: string) {
  if (!value) return "—";
  try {
    return new Date(value).toLocaleString();
  } catch {
    return value;
  }
}

function normalizeTechStack(techStack: unknown): string[] {
  if (!techStack) return [];
  if (Array.isArray(techStack)) {
    return techStack.map((item) => String(item).trim()).filter(Boolean);
  }
  if (typeof techStack === "string") {
    const trimmed = techStack.trim();
    if (!trimmed) return [];

    try {
      return normalizeTechStack(JSON.parse(trimmed));
    } catch {
      return trimmed
        .split(/[,;\n]/)
        .map((item) => item.trim())
        .filter(Boolean);
    }
  }
  if (typeof techStack === "object") {
    return Object.entries(techStack as Record<string, unknown>).flatMap(([key, value]) => {
      if (value === true) return [key];
      if (typeof value === "string" && value.trim()) return [value.trim()];
      return [];
    });
  }
  return [String(techStack)];
}

function statusVariant(status?: string): "default" | "secondary" | "destructive" | "outline" {
  switch (status) {
    case "COMPLETED":
      return "default";
    case "PROCESSING":
    case "PENDING":
    case "CLONING":
    case "ANALYZING":
      return "secondary";
    case "FAILED":
      return "destructive";
    default:
      return "outline";
  }
}

function isGithubUrl(url?: string | null): boolean {
  if (!url) return false;
  return /^https?:\/\/(www\.)?github\.com\//i.test(url.trim());
}

export function RepoCard({ repo, onDelete }: Props) {
  const techStack = normalizeTechStack(repo.analysis?.techStack);
  const isGithub = isGithubUrl(repo.githubUrl);
  return (
    <motion.div
      layout
      initial={{ opacity: 0, y: 8 }}
      animate={{ opacity: 1, y: 0 }}
      whileHover={{ y: -2 }}
      transition={{ duration: 0.2 }}
      className="surface-card p-5 flex flex-col gap-3 hover:shadow-soft transition-shadow"
    >
      <div className="flex items-start justify-between gap-3">
        <div className="min-w-0">
          <div className="flex items-center gap-2">
            {isGithub ? (
              <Github className="h-5 w-5 text-muted-foreground" />
            ) : (
              <FileArchive className="h-5 w-5 text-muted-foreground" />
            )}
            <h3 className="font-semibold truncate">{repo.githubUrl || repo.id}</h3>
          </div>
          {isGithub ? (
            <a
              href={repo.githubUrl}
              target="_blank"
              rel="noreferrer"
              className="text-xs text-muted-foreground hover:text-foreground truncate mt-0.5 inline-block"
            >
              {repo.githubUrl}
            </a>
          ) : (
            <p className="text-xs text-muted-foreground truncate mt-0.5">{repo.githubUrl}</p>
          )}
        </div>
        <Badge variant={statusVariant(repo.status)}>{repo.status ?? "UNKNOWN"}</Badge>
      </div>

      <dl className="grid gap-2 text-xs text-muted-foreground">
        <div className="flex items-center gap-1">
          <Clock className="h-3 w-3" />
          <span>Created: {formatDate(repo.createdAt ?? undefined)}</span>
        </div>
        <div className="flex items-start gap-2">
          <Tags className="mt-0.5 h-3 w-3 shrink-0" />
          <div className="flex flex-wrap gap-1">
            {techStack.length > 0 ? (
              techStack.slice(0, 6).map((item) => (
                <Badge key={item} variant="outline" className="px-2 py-0 text-[10px] uppercase tracking-wide">
                  {item}
                </Badge>
              ))
            ) : (
              <span>Tech stack unavailable</span>
            )}
          </div>
        </div>
      </dl>

      <div className="flex items-center gap-2 pt-1">
        <Button asChild size="sm" className="flex-1">
          <Link to="/repository/$id" params={{ id: repo.id }}>
            <ExternalLink className="h-4 w-4" />
            Open
          </Link>
        </Button>
        <Button
          variant="outline"
          size="sm"
          onClick={() => onDelete?.(repo.id)}
          aria-label="Delete repository"
        >
          <Trash2 className="h-4 w-4" />
        </Button>
      </div>
    </motion.div>
  );
}
