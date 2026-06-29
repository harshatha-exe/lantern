import { useState } from "react";
import { useNavigate } from "@tanstack/react-router";
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription } from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Button } from "@/components/ui/button";
import { useAuth } from "@/contexts/AuthContext";
import { extractErrorMessage } from "@/api/client";
import { toast } from "sonner";
import { Loader2 } from "lucide-react";

type Mode = "login" | "register";

interface AuthDialogProps {
  open: boolean;
  mode: Mode;
  onOpenChange: (open: boolean) => void;
  onSwitchMode: (m: Mode) => void;
}

export function AuthDialog({ open, mode, onOpenChange, onSwitchMode }: AuthDialogProps) {
  const { login, register } = useAuth();
  const navigate = useNavigate();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [submitting, setSubmitting] = useState(false);

  const isLogin = mode === "login";

  async function onSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!email.trim() || !password) {
      toast.error("Please enter email and password");
      return;
    }
    setSubmitting(true);
    try {
      if (isLogin) await login(email.trim(), password);
      else await register(email.trim(), password);
      toast.success(isLogin ? "Welcome back" : "Account created");
      onOpenChange(false);
      setEmail("");
      setPassword("");
      navigate({ to: "/dashboard" });
    } catch (err) {
      toast.error(extractErrorMessage(err));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle className="text-2xl">
            {isLogin ? "Welcome back" : "Create your account"}
          </DialogTitle>
          <DialogDescription>
            {isLogin
              ? "Sign in to continue analyzing repositories."
              : "Start analyzing your codebases in seconds."}
          </DialogDescription>
        </DialogHeader>
        <form onSubmit={onSubmit} className="space-y-4 pt-2">
          <div className="space-y-2">
            <Label htmlFor="email">Email</Label>
            <Input
              id="email"
              type="email"
              autoComplete="email"
              required
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="you@example.com"
            />
          </div>
          <div className="space-y-2">
            <Label htmlFor="password">Password</Label>
            <Input
              id="password"
              type="password"
              autoComplete={isLogin ? "current-password" : "new-password"}
              required
              minLength={6}
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="••••••••"
            />
          </div>
          <Button type="submit" className="w-full" disabled={submitting}>
            {submitting && <Loader2 className="h-4 w-4 animate-spin" />}
            {isLogin ? "Login" : "Sign Up"}
          </Button>
          <p className="text-center text-sm text-muted-foreground">
            {isLogin ? "No account?" : "Already have an account?"}{" "}
            <button
              type="button"
              className="text-primary hover:underline font-medium"
              onClick={() => onSwitchMode(isLogin ? "register" : "login")}
            >
              {isLogin ? "Sign up" : "Login"}
            </button>
          </p>
        </form>
      </DialogContent>
    </Dialog>
  );
}
