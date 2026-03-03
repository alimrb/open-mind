interface Props {
  content: string;
  isStreaming: boolean;
}

export function StreamingMessage({ content, isStreaming }: Props) {
  return (
    <div className={`message assistant ${isStreaming ? 'streaming' : ''}`}>
      <div className="message-role">Assistant</div>
      <div className="message-content">
        {content}
        {isStreaming && <span className="cursor">|</span>}
      </div>
    </div>
  );
}
