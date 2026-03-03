import { McpServer } from "@modelcontextprotocol/sdk/server/mcp.js";
import { z } from "zod";
import { GALLERY_HTML } from "./gallery-html.js";

export function createGalleryServer(): McpServer {
  const server = new McpServer({
    name: "hagap-gallery",
    version: "1.0.0",
  });

  // Register the gallery UI resource
  server.resource("gallery-ui", "ui://hagap-gallery/gallery.html", async () => ({
    contents: [
      {
        uri: "ui://hagap-gallery/gallery.html",
        mimeType: "text/html",
        text: GALLERY_HTML,
      },
    ],
  }));

  // Register the show_gallery tool
  server.tool(
    "show_gallery",
    "Display a gallery of images extracted from knowledge base sources. Always call this after answering a RAG-based question.",
    {
      images: z
        .array(
          z.object({
            url: z.string().describe("Image URL"),
            alt: z.string().optional().describe("Alt text for the image"),
            caption: z.string().optional().describe("Caption to display below the image"),
            source: z.string().optional().describe("Source file or page the image came from"),
          })
        )
        .describe("Array of images to display in the gallery"),
    },
    async ({ images }) => {
      const payload = JSON.stringify({ images });

      return {
        content: [
          {
            type: "text",
            text: `Gallery rendered with ${images.length} image(s).`,
          },
          {
            type: "resource",
            resource: {
              uri: "ui://hagap-gallery/gallery.html",
              mimeType: "text/html",
              text: GALLERY_HTML,
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
