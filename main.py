import re
from knowledge_base import KNOWLEDGE_BASE
from rag_engine import search_codebase, format_results, ingest_codebase

def clean_input(text: str) -> str:
    """Strips punctuation, lowers case, and removes repetitive question prefixes."""
    text = text.lower().strip()
    
    # Strip non-alphanumeric characters except whitespace
    text = re.sub(r'[^\w\s]', '', text)
    
    # Common conversational fillers to remove automatically
    prefixes = [
        r"^can you tell me about", r"^can you explain", r"^tell me about", 
        r"^what do you know about", r"^what do you mean by", r"^explain to me",
        r"^explain", r"^define", r"^what is a", r"^what is an", r"^what is", 
        r"^what are", r"^who is", r"^how does"
    ]
    
    changed = True
    while changed:
        old_text = text
        for prefix in prefixes:
            text = re.sub(prefix, '', text).strip()
        changed = (old_text != text)
        
    return text

CODE_TRIGGERS = {"code", "function", "class", "import", "def ", "python", "script", "file", "kuzmix", "rag", "main", "knowledge_base", "how does", "what does", "explain"}

def is_code_question(text: str) -> bool:
    cleaned = text.lower()
    return any(t in cleaned for t in CODE_TRIGGERS)


def get_response(user_input: str) -> str:
    """Matches user query against key variations and canonical response mappings."""
    cleaned = clean_input(user_input)
    
    # 1. Direct match in variation mapping
    if cleaned in VARIATIONS:
        target_key = VARIATIONS[cleaned]
        return RESPONSES[target_key]
        
    # 2. Sub-string matching across mapped variations
    for trigger, key in VARIATIONS.items():
        if trigger in cleaned or cleaned in trigger:
            return RESPONSES[key]

    # 3. Direct match across canonical response keys
    if cleaned in RESPONSES:
        return RESPONSES[cleaned]

    # 4. Codebase RAG search for code-related questions
    if is_code_question(user_input):
        matches = search_codebase(user_input, top_k=3)
        if matches:
            return format_results(matches)

    # Fallback response when no match is found
    return "I don't have that specific topic in my local data yet. Ask me about Biology, Chemistry, Agricultural Science, CRS, or general knowledge!"

def run_bot():
    print("==================================================")
    print("   Kuzmix AI Local Engine Loaded Successfully")
    print("   Indexing codebase...")
    count = ingest_codebase(".")
    print(f"   Indexed {count} code chunks")
    print("   Type 'exit', 'quit', or 'bye' to stop.")
    print("==================================================\n")
    
    while True:
        try:
            user_query = input("You: ")
            if user_query.strip().lower() in ["exit", "quit", "bye"]:
                print(f"Kuzmix AI: {RESPONSES['farewell']}")
                break
            
            if not user_query.strip():
                continue
                
            reply = get_response(user_query)
            print(f"Kuzmix AI: {reply}\n")
            
        except (KeyboardInterrupt, EOFError):
            print(f"\nKuzmix AI: {RESPONSES['farewell']}")
            break

if __name__ == "__main__":
    run_bot()