/**
 * VoiceSong Studio - Interactive Documentation & Web Audio Playground
 */

(function () {
  'use strict';

  // ==================== WEB AUDIO ENGINE ====================
  let audioCtx = null;
  let analyser = null;
  let isCanvasRunning = false;

  function initAudioContext() {
    if (!audioCtx) {
      const AudioContextClass = window.AudioContext || window.webkitAudioContext;
      audioCtx = new AudioContextClass();

      analyser = audioCtx.createAnalyser();
      analyser.fftSize = 512;
      analyser.smoothingTimeConstant = 0.85;
      analyser.connect(audioCtx.destination);

      startOscilloscope();
    }
    if (audioCtx.state === 'suspended') {
      audioCtx.resume();
    }
    return audioCtx;
  }

  // --- HTML5 Canvas Oscilloscope ---
  function startOscilloscope() {
    const canvas = document.getElementById('oscilloscope');
    if (!canvas || isCanvasRunning) return;
    const ctx = canvas.getContext('2d');
    isCanvasRunning = true;

    const bufferLength = analyser.frequencyBinCount;
    const dataArray = new Uint8Array(bufferLength);

    function draw() {
      requestAnimationFrame(draw);

      // Handle high DPI
      const width = canvas.width = canvas.clientWidth * window.devicePixelRatio;
      const height = canvas.height = canvas.clientHeight * window.devicePixelRatio;

      analyser.getByteTimeDomainData(dataArray);

      ctx.fillStyle = '#090a10';
      ctx.fillRect(0, 0, width, height);

      // Center subtle reference line
      ctx.lineWidth = 1;
      ctx.strokeStyle = 'rgba(255, 255, 255, 0.08)';
      ctx.beginPath();
      ctx.moveTo(0, height / 2);
      ctx.lineTo(width, height / 2);
      ctx.stroke();

      // Waveform line
      ctx.lineWidth = 3 * window.devicePixelRatio;
      ctx.strokeStyle = '#00cec9';
      ctx.shadowBlur = 12;
      ctx.shadowColor = '#00cec9';
      ctx.beginPath();

      const sliceWidth = width / bufferLength;
      let x = 0;

      for (let i = 0; i < bufferLength; i++) {
        const v = dataArray[i] / 128.0;
        const y = (v * height) / 2;

        if (i === 0) {
          ctx.moveTo(x, y);
        } else {
          ctx.lineTo(x, y);
        }
        x += sliceWidth;
      }

      ctx.lineTo(width, height / 2);
      ctx.stroke();
      ctx.shadowBlur = 0;
    }

    draw();
  }

  // ==================== SYNTHESIZERS ====================

  /**
   * Erhu (二胡): Sawtooth oscillator + 1050Hz/3200Hz Formant bandpass + 5.2Hz Vibrato
   */
  function playErhu(freq = 293.66 /* D4 */, duration = 1.2) {
    const ctx = initAudioContext();
    const now = ctx.currentTime;

    const osc = ctx.createOscillator();
    osc.type = 'sawtooth';
    osc.frequency.setValueAtTime(freq, now);

    // Expressive 5.2Hz vibrato
    const vibrato = ctx.createOscillator();
    vibrato.frequency.value = 5.2;
    const vibratoGain = ctx.createGain();
    vibratoGain.gain.value = freq * 0.022; // ~38 cents vibrato
    vibrato.connect(osc.frequency);
    vibrato.start(now);
    vibrato.stop(now + duration);

    // Python-skin resonant formant filters
    const formant1 = ctx.createBiquadFilter();
    formant1.type = 'bandpass';
    formant1.frequency.value = 1050;
    formant1.Q.value = 3.5;

    const formant2 = ctx.createBiquadFilter();
    formant2.type = 'bandpass';
    formant2.frequency.value = 3200;
    formant2.Q.value = 3.0;

    const amp = ctx.createGain();
    amp.gain.setValueAtTime(0.001, now);
    amp.gain.linearRampToValueAtTime(0.35, now + 0.12); // smooth bow stroke onset
    amp.gain.exponentialRampToValueAtTime(0.001, now + duration);

    osc.connect(formant1);
    osc.connect(formant2);
    formant1.connect(amp);
    formant2.connect(amp);
    amp.connect(analyser);

    osc.start(now);
    osc.stop(now + duration);
  }

  /**
   * Guzheng (古筝): Plucked Karplus-Strong string simulation with shimmering harmonics
   */
  function playGuzheng(freq = 440 /* A4 */, duration = 1.4) {
    const ctx = initAudioContext();
    const now = ctx.currentTime;

    // Harmonic plucking cluster
    const harmonics = [1, 2.01, 3.02, 4.04];
    const gains = [0.3, 0.18, 0.1, 0.05];

    harmonics.forEach((h, idx) => {
      const osc = ctx.createOscillator();
      osc.type = idx % 2 === 0 ? 'triangle' : 'sine';
      osc.frequency.setValueAtTime(freq * h, now);

      const amp = ctx.createGain();
      amp.gain.setValueAtTime(gains[idx], now);
      amp.gain.exponentialRampToValueAtTime(0.0001, now + (duration / (idx * 0.5 + 1)));

      osc.connect(amp);
      amp.connect(analyser);

      osc.start(now);
      osc.stop(now + duration);
    });
  }

  /**
   * Dizi (笛子): Bamboo flute with buzzing dimo membrane and breath air noise
   */
  function playDizi(freq = 587.33 /* D5 */, duration = 1.0) {
    const ctx = initAudioContext();
    const now = ctx.currentTime;

    const osc = ctx.createOscillator();
    osc.type = 'sine';
    osc.frequency.setValueAtTime(freq, now);

    // Subtle 4.8 Hz flute vibrato
    const vibrato = ctx.createOscillator();
    vibrato.frequency.value = 4.8;
    const vibratoGain = ctx.createGain();
    vibratoGain.gain.value = freq * 0.012;
    vibrato.connect(osc.frequency);
    vibrato.start(now);
    vibrato.stop(now + duration);

    // Dimo buzz membrane 2nd harmonic
    const osc2 = ctx.createOscillator();
    osc2.type = 'sine';
    osc2.frequency.setValueAtTime(freq * 2, now);
    const amp2 = ctx.createGain();
    amp2.gain.value = 0.08;
    osc2.connect(amp2);

    const amp = ctx.createGain();
    amp.gain.setValueAtTime(0.001, now);
    amp.gain.linearRampToValueAtTime(0.35, now + 0.08);
    amp.gain.exponentialRampToValueAtTime(0.001, now + duration);

    osc.connect(amp);
    amp2.connect(amp);
    amp.connect(analyser);

    osc.start(now);
    osc.stop(now + duration);
    osc2.start(now);
    osc2.stop(now + duration);
  }

  /**
   * Beatbox Kick: Fast pitch sweep 160 Hz -> 48 Hz with punch
   */
  function playKick() {
    const ctx = initAudioContext();
    const now = ctx.currentTime;

    const osc = ctx.createOscillator();
    osc.type = 'sine';
    osc.frequency.setValueAtTime(160, now);
    osc.frequency.exponentialRampToValueAtTime(45, now + 0.12);

    const amp = ctx.createGain();
    amp.gain.setValueAtTime(1.0, now);
    amp.gain.exponentialRampToValueAtTime(0.001, now + 0.35);

    osc.connect(amp);
    amp.connect(analyser);

    osc.start(now);
    osc.stop(now + 0.35);
  }

  /**
   * Beatbox Snare: 180Hz body tone + shaped noise crackle
   */
  function playSnare() {
    const ctx = initAudioContext();
    const now = ctx.currentTime;

    // Body Tone
    const osc = ctx.createOscillator();
    osc.type = 'triangle';
    osc.frequency.setValueAtTime(180, now);
    osc.frequency.exponentialRampToValueAtTime(110, now + 0.1);
    const oscAmp = ctx.createGain();
    oscAmp.gain.setValueAtTime(0.5, now);
    oscAmp.gain.exponentialRampToValueAtTime(0.001, now + 0.14);
    osc.connect(oscAmp);
    oscAmp.connect(analyser);
    osc.start(now);
    osc.stop(now + 0.14);

    // Noise burst
    const bufferSize = ctx.sampleRate * 0.22;
    const buffer = ctx.createBuffer(1, bufferSize, ctx.sampleRate);
    const output = buffer.getChannelData(0);
    for (let i = 0; i < bufferSize; i++) {
      output[i] = Math.random() * 2 - 1;
    }

    const whiteNoise = ctx.createBufferSource();
    whiteNoise.buffer = buffer;

    const filter = ctx.createBiquadFilter();
    filter.type = 'bandpass';
    filter.frequency.value = 2200;
    filter.Q.value = 1.2;

    const noiseAmp = ctx.createGain();
    noiseAmp.gain.setValueAtTime(0.65, now);
    noiseAmp.gain.exponentialRampToValueAtTime(0.001, now + 0.22);

    whiteNoise.connect(filter);
    filter.connect(noiseAmp);
    noiseAmp.connect(analyser);

    whiteNoise.start(now);
  }

  /**
   * Beatbox Hi-Hat: Highpass filtered metallic noise cluster
   */
  function playHiHat() {
    const ctx = initAudioContext();
    const now = ctx.currentTime;

    const bufferSize = ctx.sampleRate * 0.06;
    const buffer = ctx.createBuffer(1, bufferSize, ctx.sampleRate);
    const output = buffer.getChannelData(0);
    for (let i = 0; i < bufferSize; i++) {
      output[i] = Math.random() * 2 - 1;
    }

    const noise = ctx.createBufferSource();
    noise.buffer = buffer;

    const filter = ctx.createBiquadFilter();
    filter.type = 'highpass';
    filter.frequency.value = 7500;

    const amp = ctx.createGain();
    amp.gain.setValueAtTime(0.45, now);
    amp.gain.exponentialRampToValueAtTime(0.001, now + 0.06);

    noise.connect(filter);
    filter.connect(amp);
    amp.connect(analyser);

    noise.start(now);
  }

  /**
   * 808 Sub Bass: Deep 45Hz sub wave with warm saturation
   */
  function play808Bass(freq = 43.65 /* F1 */) {
    const ctx = initAudioContext();
    const now = ctx.currentTime;

    const osc = ctx.createOscillator();
    osc.type = 'sine';
    osc.frequency.setValueAtTime(freq * 1.5, now);
    osc.frequency.exponentialRampToValueAtTime(freq, now + 0.08);

    const amp = ctx.createGain();
    amp.gain.setValueAtTime(0.85, now);
    amp.gain.exponentialRampToValueAtTime(0.001, now + 0.9);

    osc.connect(amp);
    amp.connect(analyser);

    osc.start(now);
    osc.stop(now + 0.9);
  }

  /**
   * Grand Piano: Multi-harmonic additive synthesis with dynamic hammer strike
   */
  function playPiano(freq = 261.63 /* Middle C */) {
    const ctx = initAudioContext();
    const now = ctx.currentTime;

    const harmonics = [1, 2, 3, 4, 5];
    const weights = [0.45, 0.25, 0.12, 0.06, 0.03];

    harmonics.forEach((h, i) => {
      const osc = ctx.createOscillator();
      osc.type = 'sine';
      osc.frequency.setValueAtTime(freq * h, now);

      const amp = ctx.createGain();
      amp.gain.setValueAtTime(weights[i], now);
      amp.gain.exponentialRampToValueAtTime(0.0001, now + (1.2 / (i * 0.4 + 1)));

      osc.connect(amp);
      amp.connect(analyser);

      osc.start(now);
      osc.stop(now + 1.2);
    });
  }

  /**
   * Violin: Bowed sawtooth + wooden body resonance
   */
  function playViolin(freq = 440 /* A4 */) {
    const ctx = initAudioContext();
    const now = ctx.currentTime;

    const osc = ctx.createOscillator();
    osc.type = 'sawtooth';
    osc.frequency.setValueAtTime(freq, now);

    const vibrato = ctx.createOscillator();
    vibrato.frequency.value = 5.8;
    const vibratoGain = ctx.createGain();
    vibratoGain.gain.value = freq * 0.02;
    vibrato.connect(osc.frequency);
    vibrato.start(now);
    vibrato.stop(now + 1.1);

    const woodFilter = ctx.createBiquadFilter();
    woodFilter.type = 'bandpass';
    woodFilter.frequency.value = 460;
    woodFilter.Q.value = 2.5;

    const amp = ctx.createGain();
    amp.gain.setValueAtTime(0.001, now);
    amp.gain.linearRampToValueAtTime(0.35, now + 0.15);
    amp.gain.exponentialRampToValueAtTime(0.001, now + 1.1);

    osc.connect(woodFilter);
    woodFilter.connect(amp);
    amp.connect(analyser);

    osc.start(now);
    osc.stop(now + 1.1);
  }

  // ==================== INTERACTION BINDINGS ====================

  let currentSelectedInstrument = 'erhu';

  function attachSoundPads() {
    const pads = document.querySelectorAll('.sound-pad');
    pads.forEach(pad => {
      pad.addEventListener('click', () => {
        const type = pad.dataset.instrument;
        currentSelectedInstrument = type;

        pads.forEach(p => p.classList.remove('playing'));
        pad.classList.add('playing');
        setTimeout(() => pad.classList.remove('playing'), 250);

        switch (type) {
          case 'erhu': playErhu(); break;
          case 'guzheng': playGuzheng(); break;
          case 'dizi': playDizi(); break;
          case 'piano': playPiano(); break;
          case 'violin': playViolin(); break;
          case 'kick': playKick(); break;
          case 'snare': playSnare(); break;
          case 'hihat': playHiHat(); break;
          case 'bass': play808Bass(); break;
        }
      });
    });

    // Chinese Pentatonic Scale Note Buttons
    const notePills = document.querySelectorAll('.scale-note-pill');
    notePills.forEach(pill => {
      pill.addEventListener('click', () => {
        const freq = parseFloat(pill.dataset.freq);
        notePills.forEach(p => p.classList.remove('active'));
        pill.classList.add('active');
        setTimeout(() => pill.classList.remove('active'), 500);

        // Play note using currently selected instrument
        if (currentSelectedInstrument === 'guzheng') playGuzheng(freq);
        else if (currentSelectedInstrument === 'dizi') playDizi(freq);
        else if (currentSelectedInstrument === 'piano') playPiano(freq);
        else if (currentSelectedInstrument === 'violin') playViolin(freq);
        else if (currentSelectedInstrument === 'bass') play808Bass(freq / 4);
        else playErhu(freq); // default Erhu
      });
    });
  }

  // ==================== CODE SNIPPET COPY ====================
  function attachCopyButtons() {
    const copyBtns = document.querySelectorAll('.btn-copy');
    const toast = document.getElementById('toast');

    copyBtns.forEach(btn => {
      btn.addEventListener('click', () => {
        const container = btn.closest('.code-block-container');
        const code = container.querySelector('pre code') || container.querySelector('pre');
        if (!code) return;

        navigator.clipboard.writeText(code.innerText.trim()).then(() => {
          btn.textContent = 'Copied!';
          if (toast) {
            toast.textContent = 'Code copied to clipboard!';
            toast.classList.add('show');
            setTimeout(() => toast.classList.remove('show'), 2200);
          }
          setTimeout(() => {
            btn.textContent = 'Copy';
          }, 2000);
        });
      });
    });
  }

  // ==================== SEARCH / FILTER ====================
  function attachSearch() {
    const searchInput = document.getElementById('docs-search-input');
    const sidebarLinks = document.querySelectorAll('.sidebar-link');

    if (!searchInput) return;

    searchInput.addEventListener('input', (e) => {
      const q = e.target.value.toLowerCase().trim();
      sidebarLinks.forEach(link => {
        const text = link.textContent.toLowerCase();
        const parent = link.closest('li');
        if (text.includes(q)) {
          if (parent) parent.style.display = 'block';
        } else {
          if (parent) parent.style.display = 'none';
        }
      });
    });
  }

  // ==================== MOBILE MENU ====================
  function attachMobileMenu() {
    const menuBtn = document.getElementById('mobile-menu-toggle');
    const sidebar = document.querySelector('.docs-sidebar');

    if (menuBtn && sidebar) {
      menuBtn.addEventListener('click', () => {
        sidebar.classList.toggle('open');
      });

      // Close when clicking a link
      sidebar.querySelectorAll('a').forEach(a => {
        a.addEventListener('click', () => {
          sidebar.classList.remove('open');
        });
      });
    }
  }

  // ==================== ACTIVE SCROLLSPY ====================
  function attachScrollSpy() {
    const sections = document.querySelectorAll('h2[id], h3[id]');
    const navLinks = document.querySelectorAll('.sidebar-link');

    window.addEventListener('scroll', () => {
      let currentId = '';
      const scrollPos = window.scrollY + 120;

      sections.forEach(sec => {
        if (sec.offsetTop <= scrollPos) {
          currentId = sec.getAttribute('id');
        }
      });

      if (currentId) {
        navLinks.forEach(link => {
          link.classList.remove('active');
          if (link.getAttribute('href') === '#' + currentId) {
            link.classList.add('active');
          }
        });
      }
    });
  }

  // Initialize all components on DOM ready
  document.addEventListener('DOMContentLoaded', () => {
    attachSoundPads();
    attachCopyButtons();
    attachSearch();
    attachMobileMenu();
    attachScrollSpy();
  });

})();
