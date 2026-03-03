import { McpServer } from "@modelcontextprotocol/sdk/server/mcp.js";
import { z } from "zod";
import { REFERENCE_HTML } from "./reference-html.js";

export function createReferenceServer(): McpServer {
  const server = new McpServer({
    name: "hagap-reference",
    version: "1.0.0",
  });

  // Register the reference UI resource
  server.resource("reference-ui", "ui://hagap-reference/references.html", async () => ({
    contents: [
      {
        uri: "ui://hagap-reference/references.html",
        mimeType: "text/html",
        text: REFERENCE_HTML,
      },
    ],
  }));

  // Register the show_references tool
  server.tool(
    "show_references",
    "Display source citations and references used to construct the answer. Always call this after answering a RAG-based question.",
    {
      references: z
        .array(
          z.object({
            sourceFile: z.string().describe("Name of the source file"),
            chunkIndex: z.number().describe("Index of the chunk within the source file"),
            snippet: z.string().describe("Short text snippet from the referenced chunk"),
            score: z.number().describe("Relevance score between 0 and 1"),
            sourceUrl: z
              .string()
              .optional()
              .describe("URL to the original source page, if available"),
          })
        )
        .describe("Array of reference citations to display"),
    },
    async ({ references }) => {
      const payload = JSON.stringify({ references });

      return {
        content: [
          {
            type: "text",
            text: `References rendered with ${references.length} citation(s).`,
          },
          {
            type: "resource",
            resource: {
              uri: "ui://hagap-reference/references.html",
              mimeType: "text/html",
              text: REFERENCE_HTML,
            },
          },
        ],
        _meta: {
          appData: payload,
        },
      };
    }
  );

  return server;
}
