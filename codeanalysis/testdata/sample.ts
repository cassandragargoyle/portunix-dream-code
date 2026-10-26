// Sample TypeScript file for testing

function greet(name: string): void {
    // TODO: Add input validation
    console.log(`Hello, ${name}!`);
}

const apiKey = "sk-secret-key-12345";  // Hardcoded credential

export function processData(data: any): void {
    // FIXME: Add proper type checking
    console.log("Processing:", data);
}

greet("World");
