import { createFileRoute } from "@tanstack/react-router";
import { useEffect, useState } from "react";
import { z } from "zod";
import { motion } from "framer-motion";
import {
  Sparkles, Github, Code2, FileText, ShieldCheck, GitBranch, MessageSquare,
  Layers, ArrowRight, Zap,
} from "lucide-react";
import { Button } from "@/components/ui/button";
import { ThemeToggle } from "@/components/ThemeToggle";
import { AuthDialog } from "@/components/AuthDialog";
import { cn } from "@/lib/utils";

const searchSchema = z.object({ auth: z.enum(["login", "register"]).optional() });

export const Route = createFileRoute("/")({
  validateSearch: searchSchema,
  head: () => ({
    meta: [
      { title: "Lantern — AI Repository Analyzer" },
      { name: "description", content: "Analyze any GitHub repo or ZIP archive with AI. Architecture, dependencies, security, docs and more." },
    ],
  }),
  component: Landing,
});

const features = [
  { id: "techstack", icon: Code2, title: "Tech Stack", desc: "Languages and frameworks used in the project." },
  { id: "summary", icon: Code2, title: "Repo Summary", desc: "Concise, contextual summary for the entire repository." },
  { id: "structure", icon: Layers, title: "Project Structure", desc: "Visualize the layout of every folder and file in a clear, navigable tree." },
  { id: "readme", icon: FileText, title: "README Generator", desc: "Generate a polished README from your codebase in seconds." },
  { id: "architecture", icon: GitBranch, title: "Architecture Summary", desc: "Understand modules, layers, and how they interact at a glance." },
  { id: "chat", icon: MessageSquare, title: "Chat with your Repo", desc: "Ask natural-language questions about any part of the codebase." },
  { id: "interviewquestions", icon: Zap, title: "Interview Questions", desc: "Generate potential interview questions based on the codebase." },
  { id: "resumebullets", icon: ShieldCheck, title: "Resume Bullet Points", desc: "Generate resume bullet points based on the codebase." },
];

function Landing() {
  const search = Route.useSearch();
  const [authOpen, setAuthOpen] = useState(false);
  const [mode, setMode] = useState<"login" | "register">("login");

  useEffect(() => {
    if (search.auth) {
      setMode(search.auth);
      setAuthOpen(true);
    }
  }, [search.auth]);

  function openAuth(m: "login" | "register") {
    setMode(m);
    setAuthOpen(true);
  }

  function scrollTo(id: string) {
    document.getElementById(id)?.scrollIntoView({ behavior: "smooth", block: "start" });
  }

  return (
    <div className="min-h-screen bg-background">
      {/* Header */}
      <header className="sticky top-0 z-30 border-b border-border/60 bg-background/80 backdrop-blur">
        <div className="mx-auto flex h-14 max-w-7xl items-center justify-between px-4 sm:px-6">
          <a href="/" className="flex items-center gap-2">
            <div className="gradient-bg flex h-8 w-8 items-center justify-center rounded-lg">
              <Sparkles className="h-4 w-4 text-primary-foreground" />
            </div>
            <span className="font-semibold tracking-tight">Lantern</span>
          </a>
          <div className="flex items-center gap-1 sm:gap-2">
            <ThemeToggle />
            <Button variant="ghost" size="sm" onClick={() => openAuth("login")}>Login</Button>
            <Button size="sm" onClick={() => openAuth("register")}>Sign up</Button>
          </div>
        </div>
      </header>

      <div className="mx-auto flex max-w-7xl gap-8 px-4 sm:px-6 py-12">
        {/* Sidebar nav */}
        <aside className="hidden lg:block w-56 shrink-0 sticky top-24 self-start">
          <p className="text-xs font-semibold uppercase tracking-wider text-muted-foreground mb-3">Features</p>
          <ul className="space-y-1">
            {features.map((f) => (
              <li key={f.id}>
                <button
                  onClick={() => scrollTo(f.id)}
                  className="w-full text-left text-sm px-3 py-2 rounded-md text-muted-foreground hover:text-foreground hover:bg-muted transition-colors"
                >
                  {f.title}
                </button>
              </li>
            ))}
          </ul>
        </aside>

        {/* Main */}
        <main className="flex-1 min-w-0">
          {/* Hero */}
          <section className="text-center">
            <motion.div
              initial={{ opacity: 0, y: 12 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ duration: 0.5 }}
            >
              <span className="inline-flex items-center gap-1 rounded-full border border-border bg-surface px-3 py-1 text-xs font-medium">
                <Sparkles className="h-3 w-3 text-primary" /> put something catchy here
              </span>
              <h1 className="mt-6 text-5xl sm:text-6xl font-bold tracking-tight">
                <span className="gradient-text">Lantern</span>
              </h1>
              <p className="mt-5 max-w-2xl mx-auto text-lg text-muted-foreground">
                Drop in a GitHub URL or a ZIP file and instantly explore architecture, dependencies,
                security insights, generated docs — and chat with your codebase.
              </p>
              <div className="mt-8 flex flex-wrap justify-center gap-3">
                <Button size="lg" onClick={() => openAuth("register")} className="gap-2">
                  Get started <ArrowRight className="h-4 w-4" />
                </Button>
                <Button size="lg" variant="outline" onClick={() => scrollTo("structure")}>
                  Explore features
                </Button>
              </div>
            </motion.div>
          </section>

          {/* Per-feature sections */}
          <motion.div
              initial={{ opacity: 0, y: 12 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ duration: 0.5 }}
            >
          <div className="mt-24 space-y-16">
            <div className="text-center">
              <p className="text-xs font-semibold uppercase tracking-[0.3em] text-muted-foreground">
                Refined capabilities
              </p>
              <h2 className="mt-3 text-2xl sm:text-3xl font-semibold tracking-tight">
                A focused suite of intelligent repository insights
              </h2>
            </div>
            {features.map((f) => (
              <section key={f.id} id={f.id} className="scroll-mt-24">
                <FeatureTile feature={f} />
              </section>
            ))}
          </div>
            </motion.div>
        </main>
      </div>

      {/* Footer */}
      <footer className="border-t border-border mt-16">
        <div className="mx-auto max-w-7xl px-4 sm:px-6 py-8 flex flex-col gap-6 text-sm text-muted-foreground">
          <p>© {new Date().getFullYear()} Lantern. All rights reserved.</p>
          <div className="flex flex-wrap items-center gap-x-5 gap-y-2">
            <button className="hover:text-foreground" onClick={() => scrollTo("structure")}>Features</button>
            <a href="/dashboard" className="hover:text-foreground">Dashboard</a>
            <a href="/help" className="hover:text-foreground">Help & FAQs</a>
            <a href="/profile" className="hover:text-foreground">Profile</a>
            <a href="/settings" className="hover:text-foreground">Settings</a>
            <a href="/" className="hover:text-foreground">
              Home
            </a>
            <a href="https://github.com/harshatha-exe/lantern" className="inline-flex items-center gap-1 hover:text-foreground" target="_blank" rel="noreferrer">
              <Github className="h-4 w-4" /> GitHub
            </a>
            <Button variant="ghost" size="sm" className="h-auto p-0 text-sm text-muted-foreground hover:text-foreground" onClick={() => openAuth("login")}>
              Login
            </Button>
            <Button variant="ghost" size="sm" className="h-auto p-0 text-sm text-muted-foreground hover:text-foreground" onClick={() => openAuth("register")}>
              Sign up
            </Button>
          </div>
        </div>
      </footer>

      <AuthDialog open={authOpen} mode={mode} onOpenChange={setAuthOpen} onSwitchMode={setMode} />
    </div>
  );
}

function FeatureTile({
  feature,
  delay = 0,
  compact = false,
}: {
  feature: typeof features[number];
  delay?: number;
  compact?: boolean;
}) {
  const Icon = feature.icon;
  return (
    <motion.div
      initial={{ opacity: 0, y: 16 }}
      whileInView={{ opacity: 1, y: 0 }}
      viewport={{ once: true, margin: "-80px" }}
      transition={{ duration: 0.4, delay }}
      className={cn("surface-card p-6 transition-shadow hover:shadow-soft", !compact && "sm:p-10")}
    >
      <div className="flex items-start gap-4">
        <div className="gradient-bg flex h-10 w-10 shrink-0 items-center justify-center rounded-lg">
          <Icon className="h-5 w-5 text-primary-foreground" />
        </div>
        <div>
          <h3 className={cn("font-semibold", compact ? "text-lg" : "text-2xl")}>{feature.title}</h3>
          <p className={cn("mt-2 text-muted-foreground", compact ? "text-sm" : "text-base max-w-2xl")}>
            {feature.desc}
          </p>
        </div>
      </div>
    </motion.div>
  );
}
