import { Link, useRouterState } from "@tanstack/react-router";
import { LayoutDashboard, User, Settings, HelpCircle, LogOut, Sparkles, Menu, X } from "lucide-react";
import { useState } from "react";
import { motion, AnimatePresence } from "framer-motion";
import { Button } from "@/components/ui/button";
import { useAuth } from "@/contexts/AuthContext";
import { cn } from "@/lib/utils";

const items = [
  { to: "/dashboard", label: "Dashboard", icon: LayoutDashboard },
  { to: "/profile", label: "Profile", icon: User },
  { to: "/settings", label: "Settings", icon: Settings },
  { to: "/help", label: "Help & FAQs", icon: HelpCircle },
] as const;

function NavList({ onNavigate }: { onNavigate?: () => void }) {
  const pathname = useRouterState({ select: (s) => s.location.pathname });
  const { logout } = useAuth();
  return (
    <nav className="flex h-full flex-col gap-1 p-3">
      <Link to="/" className="flex items-center gap-2 px-3 py-4" onClick={onNavigate}>
        <div className="gradient-bg flex h-8 w-8 items-center justify-center rounded-lg">
          <Sparkles className="h-4 w-4 text-primary-foreground" />
        </div>
        <span className="font-semibold tracking-tight">Lantern</span>
      </Link>
      <div className="mt-2 flex-1 space-y-1">
        {items.map((item) => {
          const active = pathname === item.to || pathname.startsWith(item.to + "/");
          const Icon = item.icon;
          return (
            <Link
              key={item.to}
              to={item.to}
              onClick={onNavigate}
              className={cn(
                "flex items-center gap-3 rounded-md px-3 py-2 text-sm font-medium transition-colors",
                active
                  ? "bg-sidebar-accent text-sidebar-accent-foreground"
                  : "text-sidebar-foreground/70 hover:bg-sidebar-accent hover:text-sidebar-accent-foreground",
              )}
            >
              <Icon className="h-4 w-4" />
              {item.label}
            </Link>
          );
        })}
      </div>
      <Button
        variant="ghost"
        className="justify-start gap-3 text-sidebar-foreground/70 hover:text-destructive"
        onClick={() => {
          onNavigate?.();
          logout();
        }}
      >
        <LogOut className="h-4 w-4" />
        Logout
      </Button>
    </nav>
  );
}

export function Sidebar() {
  const [mobileOpen, setMobileOpen] = useState(false);
  return (
    <>
      {/* Desktop */}
      <aside className="hidden md:flex w-64 shrink-0 border-r border-sidebar-border bg-sidebar">
        <NavList />
      </aside>

      {/* Mobile trigger */}
      <Button
        variant="ghost"
        size="icon"
        className="md:hidden fixed top-3 left-3 z-40"
        onClick={() => setMobileOpen(true)}
        aria-label="Open menu"
      >
        <Menu className="h-5 w-5" />
      </Button>

      {/* Mobile drawer */}
      <AnimatePresence>
        {mobileOpen && (
          <>
            <motion.div
              className="fixed inset-0 z-50 bg-black/50 md:hidden"
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              exit={{ opacity: 0 }}
              onClick={() => setMobileOpen(false)}
            />
            <motion.aside
              className="fixed inset-y-0 left-0 z-50 w-64 bg-sidebar border-r border-sidebar-border md:hidden"
              initial={{ x: "-100%" }}
              animate={{ x: 0 }}
              exit={{ x: "-100%" }}
              transition={{ type: "spring", damping: 25, stiffness: 220 }}
            >
              <Button
                variant="ghost"
                size="icon"
                className="absolute top-3 right-3"
                onClick={() => setMobileOpen(false)}
                aria-label="Close menu"
              >
                <X className="h-5 w-5" />
              </Button>
              <NavList onNavigate={() => setMobileOpen(false)} />
            </motion.aside>
          </>
        )}
      </AnimatePresence>
    </>
  );
}
