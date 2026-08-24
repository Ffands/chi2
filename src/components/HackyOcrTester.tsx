import React, { useState, useRef, useEffect } from 'react';
import ReactCrop, { type Crop, type PixelCrop } from 'react-image-crop';
import 'react-image-crop/dist/ReactCrop.css';
import Tesseract from 'tesseract.js';

const alphabets = "АБВГДЕЁЖЗИЙКЛМНОПРСТУФХЦЧШЩЪЫЬЭЮЯабвгдеёжзийклмнопрстуфхцчшщъыьэюя0123456789 .,:;!?()-";
const templates: Array<{char: string, arr: Float32Array}> = [];

function initTemplates() {
  if (templates.length > 0) return;
  const canvas = document.createElement("canvas");
  canvas.width = 80;
  canvas.height = 80;
  const ctx = canvas.getContext("2d", { willReadFrequently: true })!;
  
  const addTemplate = (c: string, bold: boolean) => {
      if (c === ' ') {
          const arr = new Float32Array(100);
          for (let i=0; i<100; i++) arr[i] = 1.0;
          templates.push({char: c, arr});
          return;
      }
      
      ctx.fillStyle = "white";
      ctx.fillRect(0, 0, 80, 80);
      ctx.fillStyle = "black";
      ctx.font = `${bold ? 'bold ' : ''}50px sans-serif`; 
      ctx.textBaseline = "alphabetic";
      ctx.fillText(c, 20, 60);
      
      const imageData = ctx.getImageData(0, 0, 80, 80);
      const data = imageData.data;
      
      let minX = 80, minY = 80, maxX = -1, maxY = -1;
      for (let y = 0; y < 80; y++) {
          for (let x = 0; x < 80; x++) {
              const idx = (y * 80 + x) * 4;
              if (data[idx] < 128) {
                  if (x < minX) minX = x;
                  if (y < minY) minY = y;
                  if (x > maxX) maxX = x;
                  if (y > maxY) maxY = y;
              }
          }
      }
      
      if (minX <= maxX && minY <= maxY) {
          minX = Math.max(0, minX - 1);
          minY = Math.max(0, minY - 1);
          maxX = Math.min(80 - 1, maxX + 2);
          maxY = Math.min(80 - 1, maxY + 2);
          
          const w = maxX - minX;
          const h = maxY - minY;
          if (w > 0 && h > 0) {
              const cropCanvas = document.createElement("canvas");
              cropCanvas.width = w;
              cropCanvas.height = h;
              const cropCtx = cropCanvas.getContext("2d")!;
              cropCtx.drawImage(canvas, minX, minY, w, h, 0, 0, w, h);
              
              const resizeCanvas = document.createElement("canvas");
              resizeCanvas.width = 10;
              resizeCanvas.height = 10;
              const resizeCtx = resizeCanvas.getContext("2d", { willReadFrequently: true })!;
              resizeCtx.drawImage(cropCanvas, 0, 0, w, h, 0, 0, 10, 10);
              
              const resizeData = resizeCtx.getImageData(0, 0, 10, 10).data;
              const arr = new Float32Array(100);
              for (let i = 0; i < 100; i++) {
                  const r = resizeData[i * 4];
                  const g = resizeData[i * 4 + 1];
                  const b = resizeData[i * 4 + 2];
                  const lum = (r + g + b) / 3.0;
                  arr[i] = lum / 255.0;
              }
              templates.push({char: c, arr});
          }
      }
  }
  
  for (let c of alphabets) {
      addTemplate(c, false);
      addTemplate(c, true);
  }
}

export default function HackyOcrTester() {
  const [imageSrc, setImageSrc] = useState<string | null>(null);
  const [crop, setCrop] = useState<Crop>();
  const [completedCrop, setCompletedCrop] = useState<PixelCrop | null>(null);
  
  const [resultText, setResultText] = useState("");
  const [tesseractText, setTesseractText] = useState("");
  const [isProcessing, setIsProcessing] = useState(false);
  const [debugParts, setDebugParts] = useState<{char: string, imgData: string, diff: number}[]>([]);
  
  const imgRef = useRef<HTMLImageElement>(null);
  const binarizedCanvasRef = useRef<HTMLCanvasElement>(null);
  const croppedPreviewRef = useRef<HTMLCanvasElement>(null);

  useEffect(() => {
    initTemplates();
  }, []);

  const handleImageUpload = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    const reader = new FileReader();
    reader.onload = (event) => {
      setImageSrc(event.target?.result as string);
      setCompletedCrop(null);
      setCrop(undefined);
      setResultText("");
      setTesseractText("");
      setDebugParts([]);
    };
    reader.readAsDataURL(file);
  };

  const getCroppedImg = (image: HTMLImageElement, crop: PixelCrop): HTMLCanvasElement | null => {
    const canvas = document.createElement('canvas');
    const scaleX = image.naturalWidth / image.width;
    const scaleY = image.naturalHeight / image.height;
    canvas.width = crop.width;
    canvas.height = crop.height;
    const ctx = canvas.getContext('2d');

    if (!ctx) return null;

    ctx.drawImage(
      image,
      crop.x * scaleX,
      crop.y * scaleY,
      crop.width * scaleX,
      crop.height * scaleY,
      0,
      0,
      crop.width,
      crop.height
    );
    return canvas;
  };

  const processImage = async () => {
    if (!imgRef.current) return;
    setIsProcessing(true);
    setTesseractText("");
    setResultText("");
    setDebugParts([]);

    try {
      let sourceCanvas: HTMLCanvasElement;
      
      if (completedCrop && completedCrop.width > 0 && completedCrop.height > 0) {
        const cropped = getCroppedImg(imgRef.current, completedCrop);
        if (!cropped) {
            setResultText("Ошибка: не удалось вырезать область");
            setIsProcessing(false);
            return;
        }
        sourceCanvas = cropped;
      } else {
        sourceCanvas = document.createElement('canvas');
        sourceCanvas.width = imgRef.current.naturalWidth || imgRef.current.width;
        sourceCanvas.height = imgRef.current.naturalHeight || imgRef.current.height;
        const ctx = sourceCanvas.getContext('2d');
        ctx?.drawImage(imgRef.current, 0, 0, sourceCanvas.width, sourceCanvas.height);
      }

      if (sourceCanvas.width === 0 || sourceCanvas.height === 0) {
          setResultText("Ошибка: нулевой размер холста");
          setIsProcessing(false);
          return;
      }

      // Show cropped preview
      if (croppedPreviewRef.current) {
          croppedPreviewRef.current.width = sourceCanvas.width;
          croppedPreviewRef.current.height = sourceCanvas.height;
          const ctx = croppedPreviewRef.current.getContext('2d');
          if (ctx) {
             ctx.fillStyle = 'black';
             ctx.fillRect(0, 0, sourceCanvas.width, sourceCanvas.height);
             ctx.drawImage(sourceCanvas, 0, 0);
          }
      }

      const tesseractPromise = Tesseract.recognize(sourceCanvas.toDataURL(), 'rus+eng', {
        logger: m => console.log(m)
      }).then(({ data: { text } }) => {
        setTesseractText(text.trim());
      }).catch(err => {
        console.error(err);
        setTesseractText("Ошибка Tesseract: " + String(err));
      });

      const origCtx = sourceCanvas.getContext("2d", { willReadFrequently: true }) || sourceCanvas.getContext("2d")!;
      const imageData = origCtx.getImageData(0, 0, sourceCanvas.width, sourceCanvas.height);
      const data = imageData.data;
      
      // K-Means clustering (K=2) for low contrast (green on blue, etc.)
      let minLum = 255, maxLum = 0;
      let minIdx = 0, maxIdx = 0;
      for (let i = 0; i < data.length; i += 4) {
          const lum = data[i] * 0.299 + data[i+1] * 0.587 + data[i+2] * 0.114;
          if (lum < minLum) { minLum = lum; minIdx = i; }
          if (lum > maxLum) { maxLum = lum; maxIdx = i; }
      }
      
      let c1 = [data[minIdx], data[minIdx+1], data[minIdx+2]];
      let c2 = [data[maxIdx], data[maxIdx+1], data[maxIdx+2]];

      for (let iter = 0; iter < 5; iter++) {
          let sum1 = [0,0,0], count1 = 0;
          let sum2 = [0,0,0], count2 = 0;
          for (let i = 0; i < data.length; i += 4) {
              const r = data[i], g = data[i+1], b = data[i+2];
              const d1 = (r-c1[0])**2 + (g-c1[1])**2 + (b-c1[2])**2;
              const d2 = (r-c2[0])**2 + (g-c2[1])**2 + (b-c2[2])**2;
              if (d1 < d2) {
                  sum1[0]+=r; sum1[1]+=g; sum1[2]+=b; count1++;
              } else {
                  sum2[0]+=r; sum2[1]+=g; sum2[2]+=b; count2++;
              }
          }
          if (count1 > 0) { c1[0] = sum1[0]/count1; c1[1] = sum1[1]/count1; c1[2] = sum1[2]/count1; }
          if (count2 > 0) { c2[0] = sum2[0]/count2; c2[1] = sum2[1]/count2; c2[2] = sum2[2]/count2; }
      }

      let c1Count = 0, c2Count = 0;
      for (let i = 0; i < data.length; i += 4) {
          const r = data[i], g = data[i+1], b = data[i+2];
          const d1 = (r-c1[0])**2 + (g-c1[1])**2 + (b-c1[2])**2;
          const d2 = (r-c2[0])**2 + (g-c2[1])**2 + (b-c2[2])**2;
          if (d1 < d2) c1Count++; else c2Count++;
      }
      
      let bgCenter = c1, fgCenter = c2;
      if (c1Count > c2Count) {
          bgCenter = c1; fgCenter = c2;
      } else {
          bgCenter = c2; fgCenter = c1;
      }

      const binCanvas = binarizedCanvasRef.current;
      if (binCanvas) {
          binCanvas.width = sourceCanvas.width;
          binCanvas.height = sourceCanvas.height;
          const binCtx = binCanvas.getContext("2d", { willReadFrequently: true }) || binCanvas.getContext("2d")!;
          const binImageData = binCtx.createImageData(sourceCanvas.width, sourceCanvas.height);
          const bData = binImageData.data;

          const bin2D: boolean[][] = Array.from({length: sourceCanvas.height}, () => new Array(sourceCanvas.width).fill(false));

          for (let y = 0; y < sourceCanvas.height; y++) {
            for (let x = 0; x < sourceCanvas.width; x++) {
              const idx = (y * sourceCanvas.width + x) * 4;
              const r = data[idx], g = data[idx+1], b = data[idx+2];
              const dBg = (r-bgCenter[0])**2 + (g-bgCenter[1])**2 + (b-bgCenter[2])**2;
              const dFg = (r-fgCenter[0])**2 + (g-fgCenter[1])**2 + (b-fgCenter[2])**2;
              
              const isText = dFg < dBg;
              
              if (isText) {
                bData[idx] = 0; bData[idx+1] = 0; bData[idx+2] = 0; bData[idx+3] = 255;
                bin2D[y][x] = true;
              } else {
                bData[idx] = 255; bData[idx+1] = 255; bData[idx+2] = 255; bData[idx+3] = 255;
                bin2D[y][x] = false;
              }
            }
          }
          binCtx.putImageData(binImageData, 0, 0);

          const colHisto = new Int32Array(sourceCanvas.width);
          for (let x = 0; x < sourceCanvas.width; x++) {
            let count = 0;
            for (let y = 0; y < sourceCanvas.height; y++) {
               if (bin2D[y][x]) count++;
            }
            colHisto[x] = count;
          }

          const chars: {left: number, top: number, right: number, bottom: number}[] = [];
          let inChar = false;
          let startX = 0;

          const addSegment = (sx: number, ex: number) => {
            let minY = sourceCanvas.height;
            let maxY = -1;
            for (let cy = 0; cy < sourceCanvas.height; cy++) {
              for (let cx = sx; cx < ex; cx++) {
                 if (bin2D[cy][cx]) {
                     if (cy < minY) minY = cy;
                     if (cy > maxY) maxY = cy;
                 }
              }
            }
            if (minY <= maxY) {
               chars.push({left: sx, top: minY, right: ex, bottom: maxY + 1});
            }
          };

          for (let x = 0; x < sourceCanvas.width; x++) {
             if (colHisto[x] > 0) {
                 if (!inChar) {
                     inChar = true;
                     startX = x;
                 }
             } else {
                 if (inChar) {
                     inChar = false;
                     addSegment(startX, x);
                 }
             }
          }
          if (inChar) {
              addSegment(startX, sourceCanvas.width);
          }

          let sb = "";
          let prevRect: typeof chars[0] | null = null;
          const parts: typeof debugParts = [];

          for (const rect of chars) {
              const w = rect.right - rect.left;
              const h = rect.bottom - rect.top;
              if (w <= 2 || h <= 2) continue;

              const cropCanvas = document.createElement("canvas");
              cropCanvas.width = w;
              cropCanvas.height = h;
              const cropCtx = cropCanvas.getContext("2d", { willReadFrequently: true }) || cropCanvas.getContext("2d")!;
              cropCtx.drawImage(binCanvas, rect.left, rect.top, w, h, 0, 0, w, h);

              const resizeCanvas = document.createElement("canvas");
              resizeCanvas.width = 10;
              resizeCanvas.height = 10;
              const resizeCtx = resizeCanvas.getContext("2d", { willReadFrequently: true }) || resizeCanvas.getContext("2d")!;
              resizeCtx.drawImage(cropCanvas, 0, 0, w, h, 0, 0, 10, 10);
              
              const debugImgData = resizeCanvas.toDataURL();

              const resizeData = resizeCtx.getImageData(0, 0, 10, 10).data;
              const testArr = new Float32Array(100);
              for (let i = 0; i < 100; i++) {
                  const r = resizeData[i * 4];
                  const g = resizeData[i * 4 + 1];
                  const b = resizeData[i * 4 + 2];
                  testArr[i] = ((r + g + b) / 3.0) / 255.0;
              }

              let bestMatch = '?';
              let bestDiff = Number.MAX_VALUE;

              for (const tp of templates) {
                  let diff = 0;
                  for (let i = 0; i < 100; i++) {
                      const d = testArr[i] - tp.arr[i];
                      diff += d * d;
                  }
                  if (diff < bestDiff) {
                      bestDiff = diff;
                      bestMatch = tp.char;
                  }
              }

              if (prevRect) {
                  const gap = rect.left - prevRect.right;
                  if (gap > (prevRect.right - prevRect.left) * 0.6) {
                      sb += " ";
                      parts.push({char: ' ', imgData: '', diff: 0});
                  }
              }

              sb += bestMatch;
              parts.push({ char: bestMatch, imgData: debugImgData, diff: bestDiff });
              prevRect = rect;
          }

          setResultText(sb);
          setDebugParts(parts);
      }
      
      await tesseractPromise;
    } catch (e) {
      console.error(e);
      setResultText("Критическая ошибка: " + String(e));
    } finally {
      setIsProcessing(false);
    }
  };

  return (
    <div className="bg-neutral-900 border border-neutral-800 rounded-xl p-6 space-y-6 mt-8">
      <div className="flex justify-between items-center border-b border-neutral-800 pb-4">
        <h2 className="text-xl font-medium text-white flex items-center gap-2">
          Интерактивный OCR-тестер
        </h2>
        <div className="text-sm text-neutral-400">
          Выделите область для анализа
        </div>
      </div>

      <div className="space-y-4">
        <label className="block">
          <span className="sr-only">Choose image</span>
          <input 
            type="file" 
            accept="image/*"
            onChange={handleImageUpload}
            className="block w-full text-sm text-slate-300
              file:mr-4 file:py-2 file:px-4
              file:rounded-full file:border-0
              file:text-sm file:font-semibold
              file:bg-purple-900 file:text-purple-100
              hover:file:bg-purple-800"
          />
        </label>
        <p className="text-xs text-neutral-500">Загрузите скриншот с телефона и выделите область с текстом (как это делает автокликер).</p>
      </div>

      {imageSrc && (
        <div className="space-y-4">
          <div className="bg-black/50 p-2 rounded-lg border border-neutral-800 flex justify-center max-h-[500px] overflow-auto">
            <ReactCrop 
              crop={crop} 
              onChange={c => setCrop(c)} 
              onComplete={c => setCompletedCrop(c)}
              className="max-w-full"
            >
              <img ref={imgRef} src={imageSrc} alt="Source" style={{ maxHeight: '600px' }} />
            </ReactCrop>
          </div>
          
          <div className="flex justify-end gap-2">
            <button 
                onClick={processImage}
                disabled={isProcessing}
                className="px-4 py-2 bg-purple-600 hover:bg-purple-500 text-white rounded-md text-sm font-medium transition-colors disabled:opacity-50"
            >
                {isProcessing ? 'Анализ...' : 'Распознать выбранную область'}
            </button>
          </div>
        </div>
      )}

      {(resultText || tesseractText || isProcessing) && (
        <div className="space-y-6 pt-4 border-t border-neutral-800">
          
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            <div className="space-y-2">
              <h3 className="text-sm font-medium text-neutral-300">Оригинал (Что ушло на вход)</h3>
              <div className="bg-black/50 p-2 rounded-lg border border-neutral-800 flex justify-center">
                <canvas ref={croppedPreviewRef} className="max-w-full max-h-[150px] object-contain" />
              </div>
            </div>
            
            <div className="space-y-2">
              <h3 className="text-sm font-medium text-neutral-300">Бинаризация HackyOCR (Что ищет HackyOCR)</h3>
              <div className="bg-black/50 p-2 rounded-lg border border-neutral-800 flex justify-center">
                <canvas ref={binarizedCanvasRef} className="max-w-full max-h-[150px] object-contain bg-white" />
              </div>
            </div>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
             <div className="bg-green-900/20 border border-green-500/30 rounded-lg p-4">
                <h3 className="text-xs text-green-400 uppercase tracking-wider mb-1">Tesseract OCR (Эквивалент ML-Kit)</h3>
                <p className="text-xl font-mono text-white break-all min-h-[30px]">
                    {isProcessing ? 'Расшифровка...' : (tesseractText || 'Ничего не найдено')}
                </p>
             </div>
             
             <div className="bg-purple-900/30 border border-purple-500/50 rounded-lg p-4">
                <h3 className="text-xs text-purple-300 uppercase tracking-wider mb-1">HackyOCR (Режим поиска пикселей)</h3>
                <p className="text-xl font-mono text-white break-all min-h-[30px]">
                    {resultText || 'Ничего не найдено'}
                </p>
             </div>
          </div>

          {debugParts.length > 0 && (
              <div className="space-y-2">
                <h3 className="text-sm font-medium text-neutral-300">Посимвольный разбор HackyOCR</h3>
                <div className="flex flex-wrap gap-2">
                  {debugParts.map((part, idx) => (
                    <div key={idx} className="flex flex-col items-center bg-black/40 border border-neutral-800 p-2 rounded min-w-[40px]">
                      {part.char === ' ' ? (
                        <div className="h-[20px] mb-2 text-xs text-neutral-600">[ПР]</div>
                      ) : (
                        <>
                          <img src={part.imgData} alt={part.char} className="w-[20px] h-[20px] image-rendering-pixelated mb-1 bg-white" style={{ imageRendering: 'pixelated' }} />
                          <div className="text-[10px] text-neutral-500 leading-none">MSE</div>
                          <div className="text-xs text-neutral-400">{part.diff.toFixed(1)}</div>
                        </>
                      )}
                      <div className="font-mono text-lg font-bold text-white border-t border-neutral-800 w-full text-center pt-1 mt-1">
                        {part.char === ' ' ? '_' : part.char}
                      </div>
                    </div>
                  ))}
                </div>
              </div>
          )}
        </div>
      )}
    </div>
  );
}
