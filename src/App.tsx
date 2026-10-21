import { 
  Zap, 
  ShieldCheck, 
  Eye, 
  Sliders, 
  Layers, 
  Cpu, 
  Activity, 
  Fingerprint, 
  Coffee,
  CheckCircle2,
  Terminal
} from 'lucide-react';

export default function App() {
  return (
    <div className="min-h-screen bg-neutral-950 text-neutral-200 p-6 md:p-12 font-sans selection:bg-purple-900 selection:text-white">
      <div className="max-w-5xl mx-auto space-y-10">
        
        {/* Header */}
        <header className="space-y-4 border-b border-neutral-800 pb-8">
          <div className="flex flex-wrap items-center justify-between gap-4">
            <div className="flex items-center gap-3">
              <div className="p-2.5 bg-gradient-to-br from-purple-600 to-indigo-600 rounded-xl shadow-lg shadow-purple-950/50">
                <Zap className="w-7 h-7 text-white" />
              </div>
              <div>
                <div className="flex items-center gap-2">
                  <h1 className="text-3xl font-bold text-white tracking-tight">UpwellClick v3</h1>
                  <span className="px-2.5 py-0.5 text-xs font-semibold bg-purple-500/20 text-purple-300 border border-purple-500/30 rounded-full">
                    Android Native
                  </span>
                </div>
                <p className="text-sm text-neutral-400">
                  Продвинутый автокликер с защитой от Watchdog-перезагрузок, нативным ML Kit OCR и антидетектом
                </p>
              </div>
            </div>

            <div className="flex items-center gap-2">
              <span className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-neutral-900 border border-neutral-800 text-xs font-medium text-emerald-400">
                <span className="w-2 h-2 rounded-full bg-emerald-500 animate-pulse"></span>
                Движок готов к работе
              </span>
            </div>
          </div>
        </header>

        {/* Speed & Timings Section */}
        <section className="space-y-4">
          <div className="flex items-center gap-2">
            <Activity className="w-5 h-5 text-purple-400" />
            <h2 className="text-xl font-semibold text-white">Высокоточная калибровка частоты (CPS)</h2>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div className="bg-neutral-900/80 border border-neutral-800 rounded-xl p-5 space-y-3 relative overflow-hidden">
              <div className="flex items-center justify-between">
                <span className="text-xs font-semibold uppercase tracking-wider text-emerald-400">
                  Штатный режим (по умолчанию)
                </span>
                <span className="text-2xl font-mono font-bold text-white">~300-320 CPS</span>
              </div>
              <p className="text-sm text-neutral-400 leading-relaxed">
                Тактирование с интервалом <strong className="text-neutral-200">3.1 мс</strong>. 
                Абсолютно безопасен для любых устройств. Предотвращает переполнение буфера Binder IPC и защищает от аварийной перезагрузки системы.
              </p>
              <div className="flex items-center gap-2 text-xs text-neutral-500 pt-2 border-t border-neutral-800/80">
                <CheckCircle2 className="w-4 h-4 text-emerald-500" />
                <span>Защита от сбоя ядра включена</span>
              </div>
            </div>

            <div className="bg-neutral-900/80 border border-purple-900/40 rounded-xl p-5 space-y-3 relative overflow-hidden">
              <div className="flex items-center justify-between">
                <span className="text-xs font-semibold uppercase tracking-wider text-purple-400">
                  Турбо режим (снято ограничение)
                </span>
                <span className="text-2xl font-mono font-bold text-purple-300">~600-625 CPS</span>
              </div>
              <p className="text-sm text-neutral-400 leading-relaxed">
                Наносекундный квант <strong className="text-neutral-200">1.6 мс</strong> на базе <code className="text-xs text-purple-300 bg-purple-950/60 px-1 py-0.5 rounded">System.nanoTime()</code>. 
                Выжимает максимум скорости без зависания очередей.
              </p>
              <div className="flex items-center gap-2 text-xs text-neutral-500 pt-2 border-t border-neutral-800/80">
                <Zap className="w-4 h-4 text-purple-400" />
                <span>Максимальная пропускная способность</span>
              </div>
            </div>
          </div>
        </section>

        {/* Feature Grid */}
        <section className="space-y-4">
          <div className="flex items-center gap-2">
            <Sliders className="w-5 h-5 text-indigo-400" />
            <h2 className="text-xl font-semibold text-white">Ключевые возможности службы</h2>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            <div className="bg-neutral-900/50 border border-neutral-800 rounded-xl p-5 space-y-3">
              <div className="p-2 w-fit bg-blue-500/10 border border-blue-500/20 rounded-lg text-blue-400">
                <Eye className="w-5 h-5" />
              </div>
              <h3 className="font-medium text-white">Нативный ML Kit OCR</h3>
              <p className="text-xs text-neutral-400 leading-relaxed">
                Распознавание текста прямо на телефоне через движок Huawei / Google ML Kit. Поддерживает парсинг чисел, операторы сравнения (&gt;=, &lt;=) и буквенные множители (k, m, b).
              </p>
            </div>

            <div className="bg-neutral-900/50 border border-neutral-800 rounded-xl p-5 space-y-3">
              <div className="p-2 w-fit bg-emerald-500/10 border border-emerald-500/20 rounded-lg text-emerald-400">
                <ShieldCheck className="w-5 h-5" />
              </div>
              <h3 className="font-medium text-white">Антидетект кликов</h3>
              <p className="text-xs text-neutral-400 leading-relaxed">
                Рандомизация радиуса касания и вариативные микро-паузы для имитации поведения реального пальца. Защита от систем обнаружения автокликеров.
              </p>
            </div>

            <div className="bg-neutral-900/50 border border-neutral-800 rounded-xl p-5 space-y-3">
              <div className="p-2 w-fit bg-amber-500/10 border border-amber-500/20 rounded-lg text-amber-400">
                <Fingerprint className="w-5 h-5" />
              </div>
              <h3 className="font-medium text-white">Мультитач и свайпы</h3>
              <p className="text-xs text-neutral-400 leading-relaxed">
                Одновременное удержание и нажатие нескольких точек через GestureDescription. Поддержка сложных составных траекторий жестов.
              </p>
            </div>

            <div className="bg-neutral-900/50 border border-neutral-800 rounded-xl p-5 space-y-3">
              <div className="p-2 w-fit bg-rose-500/10 border border-rose-500/20 rounded-lg text-rose-400">
                <Coffee className="w-5 h-5" />
              </div>
              <h3 className="font-medium text-white">Режим Caffeine</h3>
              <p className="text-xs text-neutral-400 leading-relaxed">
                Удержание экрана во включенном состоянии на время выполнения долгих сценариев без необходимости менять системные настройки таймаута экрана.
              </p>
            </div>

            <div className="bg-neutral-900/50 border border-neutral-800 rounded-xl p-5 space-y-3">
              <div className="p-2 w-fit bg-purple-500/10 border border-purple-500/20 rounded-lg text-purple-400">
                <Layers className="w-5 h-5" />
              </div>
              <h3 className="font-medium text-white">Плавающий оверлей</h3>
              <p className="text-xs text-neutral-400 leading-relaxed">
                Интерактивный плавающий HUD поверх всех окон. Гибкая настройка размера и цветов меток, скрытие кнопок панели (1🎯, 👁, 🕸, ⚡) и отладка.
              </p>
            </div>

            <div className="bg-neutral-900/50 border border-neutral-800 rounded-xl p-5 space-y-3">
              <div className="p-2 w-fit bg-cyan-500/10 border border-cyan-500/20 rounded-lg text-cyan-400">
                <Cpu className="w-5 h-5" />
              </div>
              <h3 className="font-medium text-white">Backpressure-очередь</h3>
              <p className="text-xs text-neutral-400 leading-relaxed">
                Очередь жестов удерживает строго оптимальный буфер (до 2 жестов). Нулевая утечка оперативной памяти и отсутствие пауз на сборку мусора (GC).
              </p>
            </div>
          </div>
        </section>

        {/* Technical architecture card */}
        <section className="bg-neutral-900/40 border border-neutral-800 rounded-xl p-6 space-y-4">
          <div className="flex items-center gap-2">
            <Terminal className="w-5 h-5 text-neutral-400" />
            <h3 className="text-base font-semibold text-white">Архитектура реализации</h3>
          </div>
          <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-4 gap-4 text-xs font-mono">
            <div className="p-3 bg-black/40 rounded-lg border border-neutral-800">
              <span className="text-neutral-500 block mb-1">Служба</span>
              <span className="text-purple-300">AutoClickService.kt</span>
            </div>
            <div className="p-3 bg-black/40 rounded-lg border border-neutral-800">
              <span className="text-neutral-500 block mb-1">UI Оверлея</span>
              <span className="text-indigo-300">UIManager.kt</span>
            </div>
            <div className="p-3 bg-black/40 rounded-lg border border-neutral-800">
              <span className="text-neutral-500 block mb-1">OCR Движок</span>
              <span className="text-blue-300">Huawei ML Kit</span>
            </div>
            <div className="p-3 bg-black/40 rounded-lg border border-neutral-800">
              <span className="text-neutral-500 block mb-1">Тактирование</span>
              <span className="text-emerald-300">NanoTime Pacing</span>
            </div>
          </div>
        </section>

        <footer className="pt-6 border-t border-neutral-800/80 text-center text-xs text-neutral-500">
          UpwellClick v3 • Android Accessibility Service Native Engine
        </footer>

      </div>
    </div>
  );
}
