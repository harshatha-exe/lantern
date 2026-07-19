import { createFileRoute } from "@tanstack/react-router";
import { useEffect, useState } from "react";
import { z } from "zod";
import { motion } from "framer-motion";
import {
  Github, Code2, FileText, ShieldCheck, GitBranch, MessageSquare,
  Layers, ArrowRight, Zap,
} from "lucide-react";
import { Link } from "@tanstack/react-router";
import { Button } from "@/components/ui/button";
import { ThemeToggle } from "@/components/ThemeToggle";
import { AuthDialog } from "@/components/AuthDialog";
import { Collapsible, CollapsibleContent, CollapsibleTrigger } from "@/components/ui/collapsible";
import { useAuth } from "@/contexts/AuthContext";
import { cn } from "@/lib/utils";
import icon from "@/components/lantern.png";

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
  { id: "techstack", icon: Code2, title: "Tech Stack", desc: "Not sure what powers a repository? Lantern uncovers the languages, frameworks, libraries, and tools behind the scenes." },
  { id: "summary", icon: Code2, title: "Repo Summary", desc: "Get a clear and concise summary of what the project does without spending an hour reading through files and commits." },
  { id: "structure", icon: Layers, title: "Project Structure", desc: "Lost in a maze of folders and nested directories? Lantern maps out the entire project structure so you always know where to look next." },
  { id: "readme", icon: FileText, title: "README Generator", desc: "Haven't written a README yet? We've got you covered. Generate polished, GitHub-ready documentation in seconds and let your project make a great first impression." },
  { id: "architecture", icon: GitBranch, title: "Architecture Summary", desc: "Monolith? MVC? Layered architecture? Lantern identifies patterns, highlights key relationships, and helps you understand how everything fits together." },
  { id: "chat", icon: MessageSquare, title: "Chat with your Repo", desc: "Meet Beacon, your repository companion. Ask questions in plain English and get answers grounded in your codebase." },
  { id: "interviewquestions", icon: Zap, title: "Interview Questions", desc: "Preparing to demo your project or face technical interviews? Lantern generates thoughtful questions based on your repository, so you're never caught off guard." },
  { id: "resumebullets", icon: ShieldCheck, title: "Resume Bullet Points", desc: "Built something cool but can't find the words? Turn your project into concise, impactful resume bullets that recruiters will actually want to read." },
];

function Landing() {
  const search = Route.useSearch();
  const [authOpen, setAuthOpen] = useState(false);
  const [mode, setMode] = useState<"login" | "register">("login");
  const { isAuthenticated, isReady, logout } = useAuth();

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
            <div className="flex h-15 w-15 items-center justify-center rounded-lg">
              <img src={icon} alt="" className="h-12 w-17 object-fill" />
            </div>
            <span className="font-semibold tracking-tight">Lantern</span>
          </a>
          <div className="flex items-center gap-1 sm:gap-2">
            <ThemeToggle />
            {isReady && !isAuthenticated && (
              <>
                <Button variant="ghost" size="sm" onClick={() => openAuth("login")}>Login</Button>
                <Button size="sm" onClick={() => openAuth("register")}>Sign up</Button>
              </>
            )}
          </div>
        </div>
      </header>

      <div className="mx-auto flex max-w-7xl gap-8 px-4 sm:px-6 py-12">
        {/* Sidebar nav */}
        <aside className="hidden lg:block w-56 shrink-0 sticky top-24 self-start">
          {isAuthenticated ? (
          <div className="rounded-2xl border border-border bg-surface p-3 shadow-sm min-h-[calc(100vh-7rem)] flex flex-col">
              <nav className="space-y-1">
                <SidebarLink href="/dashboard" label="Dashboard" />
                <SidebarLink href="/profile" label="Profile" />
                <SidebarLink href="/settings" label="Settings" />
                <SidebarLink href="/help" label="Help & FAQ" />
              </nav>

              <Collapsible>
                <CollapsibleTrigger className="mt-3 flex w-full items-center justify-between rounded-md px-3 py-2 text-left text-sm font-medium text-foreground transition-colors hover:bg-muted">
                  <span>Features</span>
                </CollapsibleTrigger>
                <CollapsibleContent className="pt-2">
                  <ul className="space-y-1">
                    {features.map((f) => (
                      <li key={f.id}>
                        <button
                          onClick={() => scrollTo(f.id)}
                          className="w-full rounded-md px-3 py-2 text-left text-sm text-muted-foreground transition-colors hover:bg-muted hover:text-foreground"
                        >
                          {f.title}
                        </button>
                      </li>
                    ))}
                  </ul>
                </CollapsibleContent>
              </Collapsible>
              <nav className="space-y-1"> <button type="button" onClick={logout}
                  className="flex w-full items-center rounded-md px-3 py-2 text-left text-sm font-medium text-muted-foreground transition-colors hover:bg-muted hover:text-foreground"
                >  Log out </button></nav>
            </div>
          ) : (
            <>
              <p className="mb-3 text-xs font-semibold uppercase tracking-wider text-muted-foreground">Features</p>
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
            </>
          )}
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
                <img src={icon} alt="" className="h-5 w-3 object-contain" /> Illuminate the unknown.
              </span>
              <h1 className="mt-12 text-5xl sm:text-6xl font-bold tracking-tight">
                <span className="gradient-text">Lantern - Repository Analyzer</span>
              </h1>
              <p className="mt-15 max-w-2xl mx-auto text-lg text-muted-foreground">
                Upload a repository and let Lantern illuminate its architecture, uncover its patterns, generate documentation, and guide you through every line with Beacon.
              </p>
              <div className="mt-8 flex flex-wrap justify-center gap-3">
                {!isAuthenticated && (
                  <Button size="lg" onClick={() => openAuth("register")} className="gap-2">
                    Get started <ArrowRight className="h-4 w-4" />
                  </Button>
                )}
                <Button size="lg" variant="outline" onClick={() => scrollTo("techstack")}>
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
                From Darkness to Clarity
              </p>
              <h2 className="mt-3 text-2xl sm:text-3xl font-semibold tracking-tight">
                Everything You Need to Understand a Codebase
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
            <button className="hover:text-foreground" onClick={() => scrollTo("techstack")}>Features</button>
            <a href="/help" className="hover:text-foreground">Help & FAQs</a>
            <a href="https://github.com/harshatha-exe/lantern" className="inline-flex items-center gap-1 hover:text-foreground" target="_blank" rel="noreferrer">
              <Github className="h-4 w-4" /> GitHub
            </a>
          </div>
        </div>
      </footer>

      <AuthDialog open={authOpen} mode={mode} onOpenChange={setAuthOpen} onSwitchMode={setMode} />
    </div>
  );
}

function SidebarLink({ href, label }: { href: string; label: string }) {
  return (
    <Link
      to={href}
      className="flex items-center rounded-md px-3 py-2 text-sm font-medium text-muted-foreground transition-colors hover:bg-muted hover:text-foreground"
    >
      {label}
    </Link>
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
