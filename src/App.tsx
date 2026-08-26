import React, { useState, useEffect, useRef } from 'react';
import { Play, Pause, Zap, Crosshair, Eye, FileCode, CheckCircle2, Download, RefreshCw, Layers } from 'lucide-react';

export default function App() {
  const [activeTab, setActiveTab] = useState<'overview' | 'cps' | 'scripts'>('overview');
  const [clickCount, setClickCount] = useState(0);
  const [cps, setCps] = useState(0);
  const [isCpsTesting, setIsCpsTesting] = useState(false);
  const [timeLeft, setTimeLeft] = useState(5);
  const clickTimestamps = useRef<number[]>([]);

  useEffect(() => {
    let timer: any;
    if (isCpsTesting && timeLeft > 0) {
      timer = setInterval(() => {
        setTimeLeft((prev) => prev - 1);
      }, 1000);
    } else if (timeLeft === 0 && isCpsTesting) {
      setIsCpsTesting(false);
    }
    return () => clearInterval(timer);
  }, [isCpsTesting, timeLeft]);

  const handleCpsClick = () => {
    const now = Date.now();
    clickTimestamps.current.push(now);
    // keep timestamps within 1 sec
    clickTimestamps.current = clickTimestamps.current.filter((t) => now - t <= 1000);
    setCps(clickTimestamps.current.length);
    setClickCount((prev) => prev + 1);

    if (!isCpsTesting && timeLeft > 0) {
      setIsCpsTesting(true);
    }
  };

  const resetCps = () => {
    setClickCount(0);
    setCps(0);
    setTimeLeft(5);
    setIsCpsTesting(false);
    clickTimestamps.current = [];
  };

  return (
    <div className="min-h-screen bg-zinc-950 text-zinc-100 flex flex-col font-sans">
      {/* Header */}
      <header className="border-b border-zinc-800 bg-zinc-900/60 backdrop-blur-md px-6 py-4 flex items-center justify-between sticky top-0 z-50">
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl bg-cyan-500/10 border border-cyan-500/30 flex items-center justify-center text-cyan-400 font-bold">
            ⚡
          </div>
          <div>
            <div className="flex items-center gap-2">
              <h1 className="font-bold text-lg text-white">UpwellClick v3</h1>
              <span className="text-xs px-2 py-0.5 rounded-full bg-cyan-500/20 text-cyan-400 border border-cyan-500/30 font-mono">
                v1.0.4 (Build 9)
              </span>
            </div>
            <p className="text-xs text-zinc-400">Продвинутый автокликер Android с OCR и Burst-режимом</p>
          </div>
        </div>

        <div className="flex items-center gap-2">
          <button
            onClick={() => setActiveTab('overview')}
            className={`px-3 py-1.5 rounded-lg text-xs font-medium transition ${
              activeTab === 'overview'
                ? 'bg-cyan-500 text-zinc-950 font-bold'
                : 'text-zinc-400 hover:text-zinc-200 hover:bg-zinc-800'
            }`}
          >
            Обзор и статус
          </button>
          <button
            onClick={() => setActiveTab('cps')}
            className={`px-3 py-1.5 rounded-lg text-xs font-medium transition ${
              activeTab === 'cps'
                ? 'bg-cyan-500 text-zinc-950 font-bold'
                : 'text-zinc-400 hover:text-zinc-200 hover:bg-zinc-800'
            }`}
          >
            CPS Стресс-тест
          </button>
        </div>
      </header>

      {/* Main content */}
      <main className="flex-1 max-w-5xl w-full mx-auto p-6 space-y-6">
        {activeTab === 'overview' && (
          <div className="space-y-6">
            {/* Status cards */}
            <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
              <div className="p-4 rounded-xl bg-zinc-900 border border-zinc-800 flex flex-col justify-between">
                <div>
                  <div className="flex items-center justify-between text-zinc-400 text-xs mb-2">
                    <span>СКОРОСТЬ КЛИКОВ</span>
                    <Zap className="w-4 h-4 text-cyan-400" />
                  </div>
                  <div className="text-2xl font-bold text-white">Burst Mode (30-60+ CPS)</div>
                </div>
                <p className="text-xs text-zinc-400 mt-2">
                  Обход системного ограничения Android 100ms через пакетную генерацию микро-жестов.
                </p>
              </div>

              <div className="p-4 rounded-xl bg-zinc-900 border border-zinc-800 flex flex-col justify-between">
                <div>
                  <div className="flex items-center justify-between text-zinc-400 text-xs mb-2">
                    <span>ФАНТОМНЫЕ МЕТКИ</span>
                    <Layers className="w-4 h-4 text-cyan-400" />
                  </div>
                  <div className="text-2xl font-bold text-white">Изоляция макросов</div>
                </div>
                <p className="text-xs text-zinc-400 mt-2">
                  Подсветка <span className="text-cyan-400 font-mono">Ф1, Ф2</span> пунктирным оверлеем без сбоев нумерации.
                </p>
              </div>

              <div className="p-4 rounded-xl bg-zinc-900 border border-zinc-800 flex flex-col justify-between">
                <div>
                  <div className="flex items-center justify-between text-zinc-400 text-xs mb-2">
                    <span>РАСПОЗНАВАНИЕ ТЕКСТА</span>
                    <Eye className="w-4 h-4 text-cyan-400" />
                  </div>
                  <div className="text-2xl font-bold text-white">Huawei ML OCR Latin</div>
                </div>
                <p className="text-xs text-zinc-400 mt-2">
                  Поиск текста и клик по условиям появления слов с ограничением зон поиска.
                </p>
              </div>
            </div>

            {/* Build & GitHub section */}
            <div className="p-6 rounded-2xl bg-zinc-900/70 border border-zinc-800 space-y-4">
              <div className="flex items-center justify-between">
                <h2 className="text-base font-bold text-white flex items-center gap-2">
                  <CheckCircle2 className="w-5 h-5 text-cyan-400" />
                  Готовность к сборке на GitHub Actions
                </h2>
                <span className="text-xs px-2.5 py-1 rounded-md bg-emerald-500/10 text-emerald-400 border border-emerald-500/30">
                  Все файлы проверены
                </span>
              </div>

              <p className="text-sm text-zinc-300">
                Все файлы проекта пересозданы начисто в системной файловой системе. При нажатии на вкладку{' '}
                <strong className="text-cyan-300">GitHub</strong> в верхнем меню AI Studio все файлы проекта синхронизируются в
                репозиторий и соберутся в релизный APK.
              </p>

              <div className="bg-zinc-950 p-4 rounded-xl border border-zinc-800 font-mono text-xs text-zinc-400 space-y-1">
                <div>• Version Code: <span className="text-cyan-400">9</span></div>
                <div>• Version Name: <span className="text-cyan-400">1.0.4</span></div>
                <div>• Target SDK: <span className="text-zinc-200">34 (Android 14)</span></div>
                <div>• Min SDK: <span className="text-zinc-200">30 (Android 11)</span></div>
                <div>• Workflow: <span className="text-zinc-200">.github/workflows/build.yml</span></div>
              </div>
            </div>
          </div>
        )}

        {activeTab === 'cps' && (
          <div className="space-y-6">
            <div className="p-6 rounded-2xl bg-zinc-900 border border-zinc-800 text-center space-y-6">
              <div>
                <h2 className="text-xl font-bold text-white">Интерактивный CPS клик-тест</h2>
                <p className="text-xs text-zinc-400 mt-1">
                  Проверьте частоту кликов (CPS) или испытайте автокликер прямо в окне
                </p>
              </div>

              <div className="flex justify-center gap-8 items-center">
                <div className="bg-zinc-950 px-6 py-4 rounded-xl border border-zinc-800">
                  <div className="text-3xl font-black text-cyan-400 font-mono">{cps}</div>
                  <div className="text-xs text-zinc-400 uppercase tracking-wider mt-1">Текущий CPS</div>
                </div>

                <div className="bg-zinc-950 px-6 py-4 rounded-xl border border-zinc-800">
                  <div className="text-3xl font-black text-white font-mono">{clickCount}</div>
                  <div className="text-xs text-zinc-400 uppercase tracking-wider mt-1">Всего кликов</div>
                </div>

                <div className="bg-zinc-950 px-6 py-4 rounded-xl border border-zinc-800">
                  <div className="text-3xl font-black text-amber-400 font-mono">{timeLeft}s</div>
                  <div className="text-xs text-zinc-400 uppercase tracking-wider mt-1">Таймер</div>
                </div>
              </div>

              <div className="flex justify-center">
                <button
                  onClick={handleCpsClick}
                  disabled={timeLeft === 0}
                  className={`w-64 h-36 rounded-2xl flex flex-col items-center justify-center font-bold text-lg transition shadow-xl select-none ${
                    timeLeft === 0
                      ? 'bg-zinc-800 text-zinc-500 cursor-not-allowed'
                      : 'bg-cyan-500 hover:bg-cyan-400 text-zinc-950 active:scale-95'
                  }`}
                >
                  <Zap className="w-8 h-8 mb-1" />
                  <span>КЛИКАЙ СЮДА</span>
                </button>
              </div>

              <div className="flex justify-center">
                <button
                  onClick={resetCps}
                  className="px-4 py-2 rounded-lg bg-zinc-800 hover:bg-zinc-700 text-xs text-zinc-300 flex items-center gap-2"
                >
                  <RefreshCw className="w-3.5 h-3.5" />
                  Сбросить тест
                </button>
              </div>
            </div>
          </div>
        )}
      </main>
    </div>
  );
}
