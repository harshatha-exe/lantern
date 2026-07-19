import { useCallback, useState } from "react";
import { useNavigate } from "@tanstack/react-router";
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription, DialogFooter } from "@/components/ui/dialog";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Button } from "@/components/ui/button";
import { repositoriesApi } from "@/api/repositories";
import { extractErrorMessage } from "@/api/client";
import { toast } from "sonner";
import { Github, FileArchive, Loader2, UploadCloud } from "lucide-react";
import { cn } from "@/lib/utils";

interface Props {
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

export function UploadDialog({ open, onOpenChange }: Props) {
  const navigate = useNavigate();
  const [tab, setTab] = useState<"github" | "zip">("github");
  const [githubUrl, setGithubUrl] = useState("");
  const [file, setFile] = useState<File | null>(null);
  const [dragOver, setDragOver] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  const reset = () => {
    setGithubUrl("");
    setFile(null);
    setDragOver(false);
    setSubmitting(false);
  };

  const onDrop = useCallback((e: React.DragEvent<HTMLDivElement>) => {
    e.preventDefault();
    setDragOver(false);
    const f = e.dataTransfer.files?.[0];
    if (f) {
      if (!f.name.toLowerCase().endsWith(".zip")) {
        toast.error("Only .zip files are accepted");
        return;
      }
      setFile(f);
    }
  }, []);

  async function onSubmit() {
    setSubmitting(true);
    try {
      let result;
      if (tab === "github") {
        const url = githubUrl.trim();
        if (!/^https?:\/\/(www\.)?github\.com\/.+\/.+/i.test(url)) {
          toast.error("Please enter a valid GitHub URL");
          setSubmitting(false);
          return;
        }
        result = await repositoriesApi.submitGithub({ githubUrl: url });
      } else {
        if (!file) {
          toast.error("Please select a ZIP file");
          setSubmitting(false);
          return;
        }
        result = await repositoriesApi.uploadZip(file);
      }
      const id = (result as { id?: string })?.id;
      toast.success("Repository submitted");
      onOpenChange(false);
      reset();
      if (id) navigate({ to: "/repository/$id", params: { id } });
    } catch (err) {
      toast.error(extractErrorMessage(err));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <Dialog open={open} onOpenChange={(o) => { onOpenChange(o); if (!o) reset(); }}>
      <DialogContent className="sm:max-w-lg">
        <DialogHeader>
          <DialogTitle>Upload Repository</DialogTitle>
          <DialogDescription>
            <p>Analyze a GitHub repo or upload a ZIP archive. Max repo size: 500KB.</p>
            <p className="mt-2 text-sm text-muted-foreground">
              Pro Tip: If you are analyzing a large full-stack repository, try analyzing it in pieces. Download just the frontend or backend folder as a ZIP file and upload it.
            </p>
          </DialogDescription>
        </DialogHeader>

        <Tabs value={tab} onValueChange={(v) => setTab(v as "github" | "zip")} className="mt-2">
          <TabsList className="grid w-full grid-cols-2">
            <TabsTrigger value="github"><Github className="h-4 w-4 mr-2" />GitHub URL</TabsTrigger>
            <TabsTrigger value="zip"><FileArchive className="h-4 w-4 mr-2" />ZIP File</TabsTrigger>
          </TabsList>

          <TabsContent value="github" className="space-y-3 pt-4">
            <Label htmlFor="ghurl">Repository URL</Label>
            <Input
              id="ghurl"
              placeholder="https://github.com/owner/repo"
              value={githubUrl}
              onChange={(e) => setGithubUrl(e.target.value)}
            />
          </TabsContent>

          <TabsContent value="zip" className="pt-4">
            <div
              onDragOver={(e) => { e.preventDefault(); setDragOver(true); }}
              onDragLeave={() => setDragOver(false)}
              onDrop={onDrop}
              className={cn(
                "flex flex-col items-center justify-center gap-2 rounded-lg border-2 border-dashed p-8 text-center transition-colors",
                dragOver ? "border-primary bg-primary/5" : "border-border",
              )}
            >
              <UploadCloud className="h-8 w-8 text-muted-foreground" />
              <p className="text-sm">
                {file ? <span className="font-medium">{file.name}</span> : "Drag & drop a .zip file"}
              </p>
              <p className="text-xs text-muted-foreground">or</p>
              <Label htmlFor="zipfile" className="cursor-pointer text-primary hover:underline text-sm">
                Browse files
              </Label>
              <input
                id="zipfile"
                type="file"
                accept=".zip,application/zip"
                className="hidden"
                onChange={(e) => setFile(e.target.files?.[0] ?? null)}
              />
            </div>
          </TabsContent>
        </Tabs>

        <DialogFooter className="pt-2">
          <Button variant="ghost" onClick={() => onOpenChange(false)}>Cancel</Button>
          <Button onClick={onSubmit} disabled={submitting}>
            {submitting && <Loader2 className="h-4 w-4 animate-spin" />}
            Upload Repository
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
