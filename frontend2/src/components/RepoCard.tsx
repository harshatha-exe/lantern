import { motion } from "framer-motion";
import { Github, FileArchive, Trash2, ExternalLink, Clock } from "lucide-react";
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

function statusVariant(status?: string): "default" | "secondary" | "destructive" | "outline" {
  switch (status) {
    case "COMPLETED":
      return "default";
    case "PROCESSING":
    case "PENDING":
      return "secondary";
    case "FAILED":
      return "destructive";
    default:
      return "outline";
  }
}

export function RepoCard({ repo, onDelete }: Props) {
  const isGithub = (repo.source ?? "").toUpperCase() === "GITHUB" || !!repo.githubUrl;
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
            {isGithub ? <Github className="h-4 w-4 text-muted-foreground" /> : <FileArchive className="h-4 w-4 text-muted-foreground" />}
            <h3 className="font-semibold truncate">{repo.name ?? repo.githubUrl ?? repo.id}</h3>
          </div>
          {repo.githubUrl && (
            <p className="text-xs text-muted-foreground truncate mt-0.5">{repo.githubUrl}</p>
          )}
        </div>
        <Badge variant={statusVariant(repo.status)}>{repo.status ?? "UNKNOWN"}</Badge>
      </div>

      <dl className="grid grid-cols-2 gap-2 text-xs text-muted-foreground">
        <div className="flex items-center gap-1">
          <Clock className="h-3 w-3" />
          <span>Uploaded: {formatDate(repo.uploadedAt ?? repo.createdAt)}</span>
        </div>
        <div className="flex items-center gap-1">
          <Clock className="h-3 w-3" />
          <span>Last opened: {formatDate(repo.lastOpenedAt)}</span>
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
