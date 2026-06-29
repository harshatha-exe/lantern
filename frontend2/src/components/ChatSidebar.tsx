import { useEffect, useRef, useState } from "react";
import { motion, AnimatePresence } from "framer-motion";
import { MessageSquare, Send, X, Copy, Loader2 } from "lucide-react";
import ReactMarkdown from "react-markdown";
import remarkGfm from "remark-gfm";
import { Prism as SyntaxHighlighter } from "react-syntax-highlighter";
import { oneDark } from "react-syntax-highlighter/dist/esm/styles/prism";
import { Button } from "@/components/ui/button";
import { Textarea } from "@/components/ui/textarea";
import { repositoriesApi } from "@/api/repositories";
import { extractErrorMessage } from "@/api/client";
import { toast } from "sonner";
import type { ChatMessage } from "@/types";
import { cn } from "@/lib/utils";

interface Props {
  repositoryId: string;
}

function extractAnswer(payload: Record<string, unknown>): string {
  const candidates = ["answer", "response", "message", "content", "text", "reply"];
  for (const k of candidates) {
    const v = payload[k];
    if (typeof v === "string" && v.trim()) return v;
  }
  return "```json\n" + JSON.stringify(payload, null, 2) + "\n```";
}

export function ChatSidebar({ repositoryId }: Props) {
  const [open, setOpen] = useState(false);
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [input, setInput] = useState("");
  const [sending, setSending] = useState(false);
  const scrollRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    scrollRef.current?.scrollTo({ top: scrollRef.current.scrollHeight, behavior: "smooth" });
  }, [messages, sending]);

  async function send() {
    const text = input.trim();
    if (!text || sending) return;
    const userMsg: ChatMessage = { id: crypto.randomUUID(), role: "user", content: text, createdAt: Date.now() };
    setMessages((m) => [...m, userMsg]);
    setInput("");
    setSending(true);
    try {
      const res = await repositoriesApi.chat(repositoryId, text);
      const assistant: ChatMessage = {
        id: crypto.randomUUID(),
        role: "assistant",
        content: extractAnswer(res),
        createdAt: Date.now(),
      };
      setMessages((m) => [...m, assistant]);
    } catch (err) {
      toast.error(extractErrorMessage(err));
    } finally {
      setSending(false);
    }
  }

  return (
    <>
      <motion.div className="fixed bottom-6 right-6 z-40" whileHover={{ scale: 1.05 }} whileTap={{ scale: 0.95 }}>
        <Button
          size="icon"
          onClick={() => setOpen((o) => !o)}
          className="h-14 w-14 rounded-full shadow-glow gradient-bg"
          aria-label="Chat with repository"
        >
          {open ? <X className="h-6 w-6" /> : <MessageSquare className="h-6 w-6" />}
        </Button>
      </motion.div>

      <AnimatePresence>
        {open && (
          <motion.aside
            initial={{ x: "100%", opacity: 0 }}
            animate={{ x: 0, opacity: 1 }}
            exit={{ x: "100%", opacity: 0 }}
            transition={{ type: "spring", damping: 26, stiffness: 220 }}
            className="fixed inset-y-0 right-0 z-40 flex w-full sm:w-[420px] flex-col border-l border-border bg-surface shadow-2xl"
          >
            <header className="flex items-center justify-between border-b border-border p-4">
              <div>
                <h2 className="font-semibold">Chat with repository</h2>
                <p className="text-xs text-muted-foreground">Ask questions about this codebase</p>
              </div>
              <Button variant="ghost" size="icon" onClick={() => setOpen(false)} aria-label="Close chat">
                <X className="h-4 w-4" />
              </Button>
            </header>

            <div ref={scrollRef} className="flex-1 overflow-y-auto">
              <div className="p-4 space-y-4">
                {messages.length === 0 && (
                  <p className="text-sm text-muted-foreground text-center pt-8">
                    Ask anything about this codebase — architecture, files, dependencies…
                  </p>
                )}
                {messages.map((m) => (
                  <ChatBubble key={m.id} message={m} />
                ))}
                {sending && (
                  <div className="flex items-center gap-2 text-sm text-muted-foreground">
                    <Loader2 className="h-4 w-4 animate-spin" /> Thinking…
                  </div>
                )}
              </div>
            </div>

            <div className="border-t border-border p-3">
              <div className="flex items-end gap-2">
                <Textarea
                  value={input}
                  onChange={(e) => setInput(e.target.value)}
                  placeholder="Ask a question…"
                  className="min-h-[44px] max-h-32 resize-none"
                  onKeyDown={(e) => {
                    if (e.key === "Enter" && !e.shiftKey) {
                      e.preventDefault();
                      send();
                    }
                  }}
                />
                <Button onClick={send} disabled={sending || !input.trim()} size="icon" aria-label="Send">
                  <Send className="h-4 w-4" />
                </Button>
              </div>
            </div>
          </motion.aside>
        )}
      </AnimatePresence>
    </>
  );
}

function ChatBubble({ message }: { message: ChatMessage }) {
  const isUser = message.role === "user";
  return (
    <div className={cn("flex flex-col gap-1", isUser ? "items-end" : "items-start")}>
      <div
        className={cn(
          "max-w-[85%] rounded-2xl px-4 py-2 text-sm",
          isUser ? "bg-primary text-primary-foreground" : "bg-muted text-foreground",
        )}
      >
        {isUser ? (
          <p className="whitespace-pre-wrap">{message.content}</p>
        ) : (
          <div className="prose prose-sm dark:prose-invert max-w-none">
            <ReactMarkdown
              remarkPlugins={[remarkGfm]}
              components={{
                code({ inline, className, children, ...props }: any) {
                  const match = /language-(\w+)/.exec(className || "");
                  if (!inline && match) {
                    return (
                      <SyntaxHighlighter language={match[1]} style={oneDark as any} PreTag="div" customStyle={{ borderRadius: 8, margin: 0 }}>
                        {String(children).replace(/\n$/, "")}
                      </SyntaxHighlighter>
                    );
                  }
                  return <code className={className} {...props}>{children}</code>;
                },
              }}
            >
              {message.content}
            </ReactMarkdown>
          </div>
        )}
      </div>
      {!isUser && (
        <Button
          variant="ghost"
          size="sm"
          className="h-6 px-2 text-xs text-muted-foreground"
          onClick={() => {
            navigator.clipboard.writeText(message.content);
            toast.success("Copied");
          }}
        >
          <Copy className="h-3 w-3" /> Copy
        </Button>
      )}
    </div>
  );
}
