import { Smartphone, Download, Github } from 'lucide-react';
import HackyOcrTester from './components/HackyOcrTester';

export default function App() {
  return (
    <div className="min-h-screen bg-neutral-950 text-neutral-200 p-8 font-sans">
      <div className="max-w-4xl mx-auto space-y-8">
        <header className="space-y-2">
          <h1 className="text-4xl font-semibold text-white tracking-tight">HackyOCR / ML Kit OCR</h1>
          <p className="text-neutral-400 text-lg leading-relaxed">
            Интерактивное тестирование распознавания текста. Сделайте скриншот, загрузите и проверьте техпроцесс.
          </p>
        </header>

        <HackyOcrTester />
      </div>
    </div>
  );
}
