import { createFileRoute } from "@tanstack/react-router";
import { AppLayout } from "@/layouts/AppLayout";
import { Accordion, AccordionContent, AccordionItem, AccordionTrigger } from "@/components/ui/accordion";

const faqs = [
  { q: "How do I analyze a repository?", a: "Click Upload on the dashboard and paste a GitHub URL or drop a ZIP file." },
  { q: "Is my code stored?", a: "Repositories are processed to power features like summaries, documentation, and Beacon chat, while keeping your data secure and accessible only to you. When you delete the repository from the dashboard, the analysis, code and all associated data are permanently deleted." },
  { q: "What file types are supported for upload?", a: "Only .zip archives with a maximum size of 500 KB are accepted for direct upload." },
  { q: "Why is my repository still processing?", a: "Analysis runs asynchronously on the backend. The page will refresh automatically when complete." },
  { q: "Can I chat with my repository?", a: "Yes, open any repository and use the floating button on the bottom-right to chat." },
];

export const Route = createFileRoute("/help")({
  ssr: false,
  head: () => ({ meta: [{ title: "Help & FAQ - Lantern" }] }),
  component: Help,
});

function Help() {
  return (
    <AppLayout>
      <div className="mx-auto max-w-3xl px-4 sm:px-6 py-8 space-y-6">
        <header>
          <h1 className="text-2xl font-semibold tracking-tight">Help & FAQs</h1>
          <p className="text-sm text-muted-foreground">Answers to common questions.</p>
        </header>
        <div className="surface-card p-2">
          <Accordion type="single" collapsible className="w-full">
            {faqs.map((f, i) => (
              <AccordionItem key={i} value={`item-${i}`} className="px-4">
                <AccordionTrigger className="text-left">{f.q}</AccordionTrigger>
                <AccordionContent className="text-muted-foreground">{f.a}</AccordionContent>
              </AccordionItem>
            ))}
          </Accordion>
        </div>
        <p  className="mt-20 text-sm text-muted-foreground"> Still lost in the dark? Reach out to us for help {" "}  
          <a href="https://mail.google.com/mail/?view=cm&fs=1&to=harshatharithika@gmail.com"><u>here</u></a>.</p>
      </div>
    </AppLayout>
  );
}
