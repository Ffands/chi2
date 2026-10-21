import { useState } from 'react';
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
  Terminal,
  Copy,
  Check,
  Sparkles
} from 'lucide-react';

function buildRainbowHeartJson() {
  const swatches = [
    { id: 2, x: 320, y: 2320, color: -65536, scales: [16.0, 14.2] },
    { id: 4, x: 530, y: 2320, color: -26368, scales: [12.5, 10.8] },
    { id: 6, x: 620, y: 2320, color: -256, scales: [9.2, 7.6] },
    { id: 8, x: 720, y: 2320, color: -16711936, scales: [6.2, 4.8] },
    { id: 10, x: 815, y: 2320, color: -16711681, scales: [3.6, 2.5] },
    { id: 12, x: 165, y: 2410, color: -16744193, scales: [1.7, 1.0] },
    { id: 14, x: 255, y: 2410, color: -65281, scales: [0.5] }
  ];
  const cx = 636;
  const cy = 1120;
  const heartPt = (t: number, s: number) => {
    const hx = 16 * Math.pow(Math.sin(t), 3);
    const hy = -(13 * Math.cos(t) - 5 * Math.cos(2 * t) - 2 * Math.cos(3 * t) - Math.cos(4 * t));
    return {
      x: Math.round(cx + hx * s),
      y: Math.round(cy + hy * s + (16 - s) * 2.5)
    };
  };
  const nodes: Record<string, unknown>[] = [
    { id: 1, type: 'CLICK', x: 320, y: 2670, clickDurationMs: 45, delayAfterMs: 280, crosshairColor: -65536 }
  ];
  swatches.forEach((sw, i) => {
    const drawId = sw.id + 1;
    const reopenId = 20 + i;
    nodes.push({
      id: sw.id,
      type: 'CLICK',
      x: sw.x,
      y: sw.y,
      clickDurationMs: 45,
      delayAfterMs: 240,
      crosshairColor: sw.color,
      nextNodeIdOnSuccess: drawId,
      ...(i > 0 ? { skipSequentialExecution: true } : {})
    });
    const pts: { x: number; y: number }[] = [];
    sw.scales.forEach((s) => {
      const steps = 36;
      for (let k = 0; k <= steps; k++) {
        pts.push(heartPt((2 * Math.PI * k) / steps, s));
      }
    });
    const start = pts[0];
    const end = pts[pts.length - 1];
    nodes.push({
      id: drawId,
      type: 'CLICK',
      x: start.x,
      y: start.y,
      isSwipe: true,
      swipeEndX: end.x,
      swipeEndY: end.y,
      swipeDurationMs: 1100,
      delayAfterMs: 220,
      crosshairColor: sw.color,
      swipePathPoints: pts.slice(1, -1),
      ...(i > 0 ? { skipSequentialExecution: true } : {}),
      nextNodeIdOnSuccess: i < swatches.length - 1 ? reopenId : -1
    });
    if (i < swatches.length - 1) {
      nodes.push({
        id: reopenId,
        type: 'CLICK',
        x: 320,
        y: 2670,
        clickDurationMs: 45,
        delayAfterMs: 280,
        crosshairColor: -65536,
        skipSequentialExecution: true,
        nextNodeIdOnSuccess: swatches[i + 1].id
      });
    }
  });
  return JSON.stringify({ screenWidth: 1272, screenHeight: 2800, nodes }, null, 2);
}

const SCRIPT_EXAMPLES = [
  {
    id: 'rainbow-heart',
    title: '💖 Радужное закрашенное сердце (7 цветов)',
    badge: 'Рисование • 21 шаг',
    description:
      'Послойная закраска классического параметрического сердца всеми 7 цветами палитры (красный, оранжевый, жёлтый, зелёный, голубой, синий, фиолетовый) с автоматическим переключением палитры.',
    json: buildRainbowHeartJson()
  },
  {
    id: 'complex-swipes',
    title: '🐉 Фигурные кривые свайпы (Восьмёрка + Спираль + Зигзаг)',
    badge: 'Кривые траектории • swipePathPoints',
    description:
      'Демонстрация сложных сглаженных росчерков через массив промежуточных точек swipePathPoints: петля бесконечности (∞), спираль Архимеда и зигзаг молнии.',
    json: JSON.stringify(
      {
        screenWidth: 1080,
        screenHeight: 2400,
        nodes: [
          {
            id: 1,
            type: 'CLICK',
            x: 540,
            y: 750,
            isSwipe: true,
            swipeEndX: 540,
            swipeEndY: 750,
            swipeDurationMs: 950,
            delayAfterMs: 300,
            crosshairColor: -16711681,
            swipePathPoints: Array.from({ length: 35 }, (_, i) => {
              const t = (2 * Math.PI * (i + 1)) / 36;
              return {
                x: Math.round(540 + 300 * Math.sin(t)),
                y: Math.round(750 + 160 * Math.sin(2 * t))
              };
            })
          },
          {
            id: 2,
            type: 'CLICK',
            x: 540,
            y: 1350,
            isSwipe: true,
            swipeEndX: 780,
            swipeEndY: 1350,
            swipeDurationMs: 1200,
            delayAfterMs: 300,
            crosshairColor: -65281,
            swipePathPoints: Array.from({ length: 40 }, (_, i) => {
              const k = i + 1;
              const angle = 0.35 * k;
              const r = 6 * k;
              return {
                x: Math.round(540 + r * Math.cos(angle)),
                y: Math.round(1350 + r * Math.sin(angle))
              };
            })
          },
          {
            id: 3,
            type: 'CLICK',
            x: 260,
            y: 1680,
            isSwipe: true,
            swipeEndX: 820,
            swipeEndY: 2180,
            swipeDurationMs: 650,
            delayAfterMs: 400,
            crosshairColor: -256,
            swipePathPoints: [
              { x: 820, y: 1780 },
              { x: 260, y: 1880 },
              { x: 820, y: 1980 },
              { x: 260, y: 2080 }
            ]
          }
        ]
      },
      null,
      2
    )
  },
  {
    id: 'smart-farm',
    title: '🧠 Умный авто-фарм (Менеджер + Числовой OCR + Цвет + Прокрутка)',
    badge: 'MANAGER + Smart OCR + Ветвление',
    description:
      'Узел-менеджер проверяет экран: если число в зоне OCR >= 1.5k — нажимает покупку (Шаг 4); если горит зелёная кнопка награды — забирает её (Шаг 5); если ничего не найдено — свайпает список вниз (Шаг 6) и возвращается к Менеджеру.',
    json: JSON.stringify(
      {
        screenWidth: 1080,
        screenHeight: 2400,
        nodes: [
          {
            id: 1,
            type: 'MANAGER',
            x: 540,
            y: 360,
            delayAfterMs: 250,
            crosshairColor: -256,
            managerRoutes: [
              { checkNodeId: 2, onSuccessGoToId: 4 },
              { checkNodeId: 3, onSuccessGoToId: 5 }
            ],
            nextNodeIdOnFail: 6
          },
          {
            id: 2,
            type: 'CHECK_COLOR',
            x: 540,
            y: 720,
            triggerMode: 2,
            isSmartOcr: true,
            ocrOperator: '>=',
            ocrTargetValue: 1500.0,
            textZoneStartX: 360,
            textZoneStartY: 660,
            textZoneEndX: 720,
            textZoneEndY: 780,
            skipSequentialExecution: true,
            crosshairColor: -16711681
          },
          {
            id: 3,
            type: 'CHECK_COLOR',
            x: 540,
            y: 1100,
            triggerMode: 0,
            targetColor: -16725933,
            colorTolerance: 25,
            skipSequentialExecution: true,
            crosshairColor: -16711936
          },
          {
            id: 4,
            type: 'CLICK',
            x: 540,
            y: 900,
            clickDurationMs: 40,
            delayAfterMs: 350,
            randomizeRadius: 8,
            skipSequentialExecution: true,
            nextNodeIdOnSuccess: 1,
            crosshairColor: -16711681
          },
          {
            id: 5,
            type: 'CLICK',
            x: 540,
            y: 1100,
            clickDurationMs: 45,
            delayAfterMs: 400,
            randomizeRadius: 10,
            skipSequentialExecution: true,
            nextNodeIdOnSuccess: 1,
            crosshairColor: -16711936
          },
          {
            id: 6,
            type: 'CLICK',
            x: 540,
            y: 1650,
            isSwipe: true,
            swipeEndX: 540,
            swipeEndY: 950,
            swipeDurationMs: 450,
            delayAfterMs: 500,
            skipSequentialExecution: true,
            nextNodeIdOnSuccess: 1,
            crosshairColor: -26368
          }
        ]
      },
      null,
      2
    )
  },
  {
    id: 'multitouch-combo',
    title: '⚡ Мультитач Комбо + Зажатие + Антидетект-серия',
    badge: 'Мультитач • isHold + syncWithNodeIds',
    description:
      'Одновременное удержание левого стика/триггера (Шаг 1, isHold) синхронно с правым ударом (Шаг 2, syncWithNodeIds="2"), затем 5 быстрых кликов с рандомизацией координат и пауз (Шаг 3) и диагональный свайп-рывок (Шаг 4).',
    json: JSON.stringify(
      {
        screenWidth: 1080,
        screenHeight: 2400,
        nodes: [
          {
            id: 1,
            type: 'CLICK',
            x: 260,
            y: 1700,
            isHold: true,
            holdDurationMs: 800,
            syncWithNodeIds: '2',
            delayAfterMs: 150,
            crosshairColor: -16711936
          },
          {
            id: 2,
            type: 'CLICK',
            x: 820,
            y: 1700,
            clickDurationMs: 60,
            delayAfterMs: 100,
            randomizeRadius: 12,
            crosshairColor: -65536
          },
          {
            id: 3,
            type: 'CLICK',
            x: 820,
            y: 1450,
            repetitions: 5,
            clickDurationMs: 25,
            delayAfterMs: 65,
            randomizeDelayMs: 25,
            randomizeRadius: 15,
            crosshairColor: -26368
          },
          {
            id: 4,
            type: 'CLICK',
            x: 320,
            y: 1450,
            isSwipe: true,
            swipeEndX: 680,
            swipeEndY: 1180,
            swipeDurationMs: 220,
            delayAfterMs: 300,
            crosshairColor: -16711681
          }
        ]
      },
      null,
      2
    )
  }
];

export default function App() {
  const [copiedId, setCopiedId] = useState<string | null>(null);

  const handleCopy = (id: string, json: string) => {
    navigator.clipboard.writeText(json);
    setCopiedId(id);
    setTimeout(() => setCopiedId(null), 2000);
  };

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

        {/* Complex Script Templates Section */}
        <section className="space-y-4">
          <div className="flex items-center justify-between flex-wrap gap-2">
            <div className="flex items-center gap-2">
              <Sparkles className="w-5 h-5 text-amber-400" />
              <h2 className="text-xl font-semibold text-white">Готовые примеры сложных скриптов (JSON)</h2>
            </div>
            <span className="text-xs text-neutral-400">
              Доступны также прямо внутри APK в разделе «Профили → Примеры сложных скриптов»
            </span>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {SCRIPT_EXAMPLES.map((item) => (
              <div
                key={item.id}
                className="bg-neutral-900/70 border border-neutral-800 rounded-xl p-5 flex flex-col justify-between space-y-4"
              >
                <div className="space-y-2">
                  <div className="flex items-center justify-between gap-2">
                    <span className="text-xs font-medium px-2.5 py-0.5 rounded-md bg-purple-500/10 text-purple-300 border border-purple-500/20">
                      {item.badge}
                    </span>
                    <button
                      onClick={() => handleCopy(item.id, item.json)}
                      className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-neutral-800 hover:bg-neutral-700 text-xs font-medium text-white transition-colors cursor-pointer"
                    >
                      {copiedId === item.id ? (
                        <>
                          <Check className="w-3.5 h-3.5 text-emerald-400" />
                          <span className="text-emerald-400">Скопировано</span>
                        </>
                      ) : (
                        <>
                          <Copy className="w-3.5 h-3.5 text-neutral-300" />
                          <span>Копировать JSON</span>
                        </>
                      )}
                    </button>
                  </div>
                  <h3 className="text-base font-semibold text-white pt-1">{item.title}</h3>
                  <p className="text-xs text-neutral-400 leading-relaxed">{item.description}</p>
                </div>
                <pre className="text-[11px] font-mono bg-black/50 border border-neutral-800/80 rounded-lg p-3 overflow-x-auto max-h-36 text-neutral-300">
                  {item.json}
                </pre>
              </div>
            ))}
          </div>
        </section>

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
                Интерактивный плавающий HUD поверх всех окон. Гибкая настройка кнопок панели (1🎯, 👁, 🕸, ⚡), быстрое переключение скриптов и отладка.
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
