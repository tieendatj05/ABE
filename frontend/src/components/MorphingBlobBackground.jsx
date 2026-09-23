import { useEffect, useRef } from 'react';
import * as THREE from 'three';
import { EffectComposer } from 'three/examples/jsm/postprocessing/EffectComposer.js';
import { RenderPass } from 'three/examples/jsm/postprocessing/RenderPass.js';
import { UnrealBloomPass } from 'three/examples/jsm/postprocessing/UnrealBloomPass.js';
import { ShaderPass } from 'three/examples/jsm/postprocessing/ShaderPass.js';
import { GammaCorrectionShader } from 'three/examples/jsm/shaders/GammaCorrectionShader.js';

// ---- Tham số cố định của hiệu ứng (theo đúng prompt gốc) ----
const bgColor = '#05000a';
const flameColor = '#5900ff';
const flameColor2 = '#ff00ff';
const flameAmt = 0.15;
const atmoColor = '#d400ff';
const atmoCount = 200;
const atmoSize = 18;
const atmoSpeed = 0.5;
const colorLow = '#1a0033';
const colorHigh = '#ff00ff';
const opacity = 0.4;
const pointSize = 4.0;
const brightness = 1.2;
const waveHeight = 1.2;
const flow = 0.5;
const scale = 1.0;

const BLOOM_LAYER = 1;

// Ashima/webgl-noise simplex 3D noise (MIT) - dùng để làm méo bán kính khối cầu điểm.
const SNOISE_GLSL = `
vec3 mod289(vec3 x){return x - floor(x * (1.0 / 289.0)) * 289.0;}
vec4 mod289(vec4 x){return x - floor(x * (1.0 / 289.0)) * 289.0;}
vec4 permute(vec4 x){return mod289(((x*34.0)+1.0)*x);}
vec4 taylorInvSqrt(vec4 r){return 1.79284291400159 - 0.85373472095314 * r;}

float snoise(vec3 v){
  const vec2 C = vec2(1.0/6.0, 1.0/3.0);
  const vec4 D = vec4(0.0, 0.5, 1.0, 2.0);

  vec3 i  = floor(v + dot(v, C.yyy));
  vec3 x0 = v - i + dot(i, C.xxx);

  vec3 g = step(x0.yzx, x0.xyz);
  vec3 l = 1.0 - g;
  vec3 i1 = min(g.xyz, l.zxy);
  vec3 i2 = max(g.xyz, l.zxy);

  vec3 x1 = x0 - i1 + C.xxx;
  vec3 x2 = x0 - i2 + C.yyy;
  vec3 x3 = x0 - D.yyy;

  i = mod289(i);
  vec4 p = permute(permute(permute(
            i.z + vec4(0.0, i1.z, i2.z, 1.0))
          + i.y + vec4(0.0, i1.y, i2.y, 1.0))
          + i.x + vec4(0.0, i1.x, i2.x, 1.0));

  float n_ = 0.142857142857;
  vec3 ns = n_ * D.wyz - D.xzx;

  vec4 j = p - 49.0 * floor(p * ns.z * ns.z);

  vec4 x_ = floor(j * ns.z);
  vec4 y_ = floor(j - 7.0 * x_);

  vec4 x = x_ *ns.x + ns.yyyy;
  vec4 y = y_ *ns.x + ns.yyyy;
  vec4 h = 1.0 - abs(x) - abs(y);

  vec4 b0 = vec4(x.xy, y.xy);
  vec4 b1 = vec4(x.zw, y.zw);

  vec4 s0 = floor(b0)*2.0 + 1.0;
  vec4 s1 = floor(b1)*2.0 + 1.0;
  vec4 sh = -step(h, vec4(0.0));

  vec4 a0 = b0.xzyw + s0.xzyw*sh.xxyy;
  vec4 a1 = b1.xzyw + s1.xzyw*sh.zzww;

  vec3 p0 = vec3(a0.xy, h.x);
  vec3 p1 = vec3(a0.zw, h.y);
  vec3 p2 = vec3(a1.xy, h.z);
  vec3 p3 = vec3(a1.zw, h.w);

  vec4 norm = taylorInvSqrt(vec4(dot(p0,p0), dot(p1,p1), dot(p2,p2), dot(p3,p3)));
  p0 *= norm.x; p1 *= norm.y; p2 *= norm.z; p3 *= norm.w;

  vec4 m = max(0.6 - vec4(dot(x0,x0), dot(x1,x1), dot(x2,x2), dot(x3,x3)), 0.0);
  m = m * m;
  return 42.0 * dot(m*m, vec4(dot(p0,x0), dot(p1,x1), dot(p2,x2), dot(p3,x3)));
}
`;

function createGlowTexture(hexColor) {
  const size = 128;
  const canvas = document.createElement('canvas');
  canvas.width = size;
  canvas.height = size;
  const ctx = canvas.getContext('2d');
  const gradient = ctx.createRadialGradient(size / 2, size / 2, 0, size / 2, size / 2, size / 2);
  gradient.addColorStop(0, hexColor + 'ff');
  gradient.addColorStop(0.25, hexColor + 'aa');
  gradient.addColorStop(0.55, hexColor + '33');
  gradient.addColorStop(0.8, hexColor + '00');
  gradient.addColorStop(1, hexColor + '00');
  ctx.fillStyle = gradient;
  ctx.fillRect(0, 0, size, size);
  const texture = new THREE.CanvasTexture(canvas);
  // Tắt mipmap: bo tròn radial gradient bị mipmap kéo méo thành viền vuông mờ khi thu nhỏ.
  texture.generateMipmaps = false;
  texture.minFilter = THREE.LinearFilter;
  texture.needsUpdate = true;
  return texture;
}

function withAlpha(hex, alpha) {
  const c = new THREE.Color(hex);
  const to255 = (v) => Math.round(v * 255)
    .toString(16)
    .padStart(2, '0');
  return `#${to255(c.r)}${to255(c.g)}${to255(c.b)}`;
}

function buildBlob() {
  const count = 9000;
  const positions = new Float32Array(count * 3);
  const radius = 3.2;

  // Phân bố Fibonacci sphere - phủ đều bề mặt hơn random thuần.
  const golden = Math.PI * (3 - Math.sqrt(5));
  for (let i = 0; i < count; i++) {
    const y = 1 - (i / (count - 1)) * 2;
    const r = Math.sqrt(1 - y * y);
    const theta = golden * i;
    const x = Math.cos(theta) * r;
    const z = Math.sin(theta) * r;
    positions[i * 3] = x * radius;
    positions[i * 3 + 1] = y * radius;
    positions[i * 3 + 2] = z * radius;
  }

  const geometry = new THREE.BufferGeometry();
  geometry.setAttribute('position', new THREE.BufferAttribute(positions, 3));

  const material = new THREE.ShaderMaterial({
    uniforms: {
      uTime: { value: 0 },
      uWaveHeight: { value: waveHeight },
      uFlow: { value: flow },
      uPointSize: { value: pointSize },
      uColorLow: { value: new THREE.Color(colorLow) },
      uColorHigh: { value: new THREE.Color(colorHigh) },
      uOpacity: { value: opacity },
      uBrightness: { value: brightness },
    },
    vertexShader: `
      uniform float uTime;
      uniform float uWaveHeight;
      uniform float uFlow;
      uniform float uPointSize;
      varying float vNoise;
      ${SNOISE_GLSL}
      void main() {
        float n = snoise(normalize(position) * 1.6 + uTime * uFlow);
        vNoise = n;
        vec3 displaced = position * (1.0 + n * uWaveHeight * 0.35);
        vec4 mvPosition = modelViewMatrix * vec4(displaced, 1.0);
        gl_PointSize = uPointSize * (36.0 / -mvPosition.z);
        gl_Position = projectionMatrix * mvPosition;
      }
    `,
    fragmentShader: `
      uniform vec3 uColorLow;
      uniform vec3 uColorHigh;
      uniform float uOpacity;
      uniform float uBrightness;
      varying float vNoise;
      void main() {
        float d = length(gl_PointCoord - vec2(0.5));
        if (d > 0.5) discard;
        float alpha = smoothstep(0.5, 0.0, d) * uOpacity;
        float t = clamp(vNoise * 0.5 + 0.5, 0.0, 1.0);
        vec3 color = mix(uColorLow, uColorHigh, t) * uBrightness;
        gl_FragColor = vec4(color, alpha);
      }
    `,
    transparent: true,
    depthWrite: false,
    blending: THREE.AdditiveBlending,
  });

  const points = new THREE.Points(geometry, material);
  points.scale.setScalar(scale);
  points.layers.enable(BLOOM_LAYER);
  return points;
}

function buildMotes() {
  const positions = new Float32Array(atmoCount * 3);
  const speeds = new Float32Array(atmoCount);
  for (let i = 0; i < atmoCount; i++) {
    const r = atmoSize * (0.3 + Math.random() * 0.7);
    const theta = Math.random() * Math.PI * 2;
    const phi = Math.acos(2 * Math.random() - 1);
    positions[i * 3] = r * Math.sin(phi) * Math.cos(theta);
    positions[i * 3 + 1] = r * Math.sin(phi) * Math.sin(theta);
    positions[i * 3 + 2] = r * Math.cos(phi) * 0.5 - 6;
    speeds[i] = 0.4 + Math.random() * 0.6;
  }
  const geometry = new THREE.BufferGeometry();
  geometry.setAttribute('position', new THREE.BufferAttribute(positions, 3));
  geometry.setAttribute('aSpeed', new THREE.BufferAttribute(speeds, 1));

  const material = new THREE.ShaderMaterial({
    uniforms: {
      uTime: { value: 0 },
      uSpeed: { value: atmoSpeed },
      uColor: { value: new THREE.Color(atmoColor) },
    },
    vertexShader: `
      uniform float uTime;
      uniform float uSpeed;
      attribute float aSpeed;
      void main() {
        vec3 p = position;
        p.y += sin(uTime * uSpeed * aSpeed + p.x) * 1.5;
        p.x += cos(uTime * uSpeed * aSpeed + p.z) * 1.5;
        vec4 mvPosition = modelViewMatrix * vec4(p, 1.0);
        gl_PointSize = 2.5 * (36.0 / -mvPosition.z);
        gl_Position = projectionMatrix * mvPosition;
      }
    `,
    fragmentShader: `
      uniform vec3 uColor;
      void main() {
        float d = length(gl_PointCoord - vec2(0.5));
        if (d > 0.5) discard;
        float alpha = smoothstep(0.5, 0.0, d) * 0.35;
        gl_FragColor = vec4(uColor, alpha);
      }
    `,
    transparent: true,
    depthWrite: false,
    blending: THREE.AdditiveBlending,
  });

  const points = new THREE.Points(geometry, material);
  points.layers.enable(BLOOM_LAYER);
  return points;
}

function buildCornerHaze() {
  const group = new THREE.Group();
  const configs = [
    { color: flameColor, pos: [-9, 6, -14], size: 11 },
    { color: flameColor2, pos: [9, -6, -14], size: 11 },
  ];
  for (const cfg of configs) {
    const texture = createGlowTexture(cfg.color);
    const material = new THREE.SpriteMaterial({
      map: texture,
      color: 0xffffff,
      transparent: true,
      opacity: flameAmt,
      blending: THREE.AdditiveBlending,
      depthWrite: false,
    });
    const sprite = new THREE.Sprite(material);
    sprite.position.set(...cfg.pos);
    sprite.scale.setScalar(cfg.size);
    group.add(sprite);
  }
  return group;
}

/**
 * Nền động Three.js: khối cầu điểm phát sáng méo bởi simplex noise (morphing
 * blob), bloom chọn lọc (selective bloom) làm khối cầu rực magenta trong khi
 * phần còn lại (hạt bụi tím, haze góc) chỉ có glow nhẹ. Chỉ trang trí - không
 * chứa logic nghiệp vụ, tự dọn dẹp (dispose) khi unmount.
 */
export default function MorphingBlobBackground() {
  const containerRef = useRef(null);

  useEffect(() => {
    const container = containerRef.current;
    if (!container) return undefined;

    const width = container.clientWidth || window.innerWidth;
    const height = container.clientHeight || window.innerHeight;

    const renderer = new THREE.WebGLRenderer({ antialias: true, alpha: false });
    renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2));
    renderer.setSize(width, height);
    container.appendChild(renderer.domElement);

    const scene = new THREE.Scene();
    scene.background = new THREE.Color(bgColor);

    const camera = new THREE.PerspectiveCamera(45, width / height, 0.1, 400);
    camera.position.set(0, 0, 12);

    const blob = buildBlob();
    const motes = buildMotes();
    const haze = buildCornerHaze();
    scene.add(blob, motes, haze);

    // ---- Selective bloom setup (mẫu chuẩn của three.js) ----
    const bloomLayer = new THREE.Layers();
    bloomLayer.set(BLOOM_LAYER);
    const darkMaterial = new THREE.MeshBasicMaterial({ color: 'black' });
    const materialCache = new Map();

    function darkenNonBloomed(obj) {
      if ((obj.isPoints || obj.isMesh || obj.isSprite) && bloomLayer.test(obj.layers) === false) {
        materialCache.set(obj.uuid, obj.material);
        obj.material = darkMaterial;
      }
    }
    function restoreMaterial(obj) {
      if (materialCache.has(obj.uuid)) {
        obj.material = materialCache.get(obj.uuid);
        materialCache.delete(obj.uuid);
      }
    }

    const renderPass = new RenderPass(scene, camera);

    const strongBloomPass = new UnrealBloomPass(new THREE.Vector2(width, height), 0.5, 0.6, 0.15);
    const bloomComposer = new EffectComposer(renderer);
    bloomComposer.renderToScreen = false;
    bloomComposer.addPass(renderPass);
    bloomComposer.addPass(strongBloomPass);

    const mixPass = new ShaderPass(
      new THREE.ShaderMaterial({
        uniforms: {
          baseTexture: { value: null },
          bloomTexture: { value: bloomComposer.renderTarget2.texture },
        },
        vertexShader: `
          varying vec2 vUv;
          void main() {
            vUv = uv;
            gl_Position = projectionMatrix * modelViewMatrix * vec4(position, 1.0);
          }
        `,
        fragmentShader: `
          uniform sampler2D baseTexture;
          uniform sampler2D bloomTexture;
          varying vec2 vUv;
          void main() {
            gl_FragColor = texture2D(baseTexture, vUv) + vec4(1.0) * texture2D(bloomTexture, vUv);
          }
        `,
      }),
      'baseTexture'
    );
    mixPass.needsSwap = true;

    const gammaPass = new ShaderPass(GammaCorrectionShader);
    const mildBloomPass = new UnrealBloomPass(new THREE.Vector2(width, height), 0.2, 0.2, 0.1);

    const finalComposer = new EffectComposer(renderer);
    finalComposer.addPass(renderPass);
    finalComposer.addPass(mildBloomPass);
    finalComposer.addPass(mixPass);
    finalComposer.addPass(gammaPass);

    // ---- Mouse parallax ----
    const mouse = { x: 0, y: 0 };
    function handleMouseMove(e) {
      mouse.x = (e.clientX / window.innerWidth) * 2 - 1;
      mouse.y = (e.clientY / window.innerHeight) * 2 - 1;
    }
    window.addEventListener('mousemove', handleMouseMove);

    function handleResize() {
      const w = container.clientWidth || window.innerWidth;
      const h = container.clientHeight || window.innerHeight;
      camera.aspect = w / h;
      camera.updateProjectionMatrix();
      renderer.setSize(w, h);
      bloomComposer.setSize(w, h);
      finalComposer.setSize(w, h);
    }
    window.addEventListener('resize', handleResize);

    let rafId;
    const clock = new THREE.Clock();
    function animate() {
      const t = clock.getElapsedTime();

      blob.material.uniforms.uTime.value = t;
      motes.material.uniforms.uTime.value = t;
      blob.rotation.y = t * 0.06;
      blob.rotation.x = Math.sin(t * 0.03) * 0.15;

      camera.position.x += (mouse.x * 1.2 - camera.position.x) * 0.03;
      camera.position.y += (-mouse.y * 1.2 - camera.position.y) * 0.03;
      camera.lookAt(0, 0, 0);

      scene.traverse(darkenNonBloomed);
      bloomComposer.render();
      scene.traverse(restoreMaterial);
      finalComposer.render();

      rafId = requestAnimationFrame(animate);
    }
    animate();

    return () => {
      cancelAnimationFrame(rafId);
      window.removeEventListener('mousemove', handleMouseMove);
      window.removeEventListener('resize', handleResize);
      blob.geometry.dispose();
      blob.material.dispose();
      motes.geometry.dispose();
      motes.material.dispose();
      haze.children.forEach((sprite) => {
        sprite.material.map?.dispose();
        sprite.material.dispose();
      });
      darkMaterial.dispose();
      // EffectComposer (three r0.143) không có .dispose() - tự giải phóng render
      // target của từng composer + render target nội bộ của UnrealBloomPass.
      bloomComposer.renderTarget1.dispose();
      bloomComposer.renderTarget2.dispose();
      finalComposer.renderTarget1.dispose();
      finalComposer.renderTarget2.dispose();
      strongBloomPass.dispose();
      mildBloomPass.dispose();
      renderer.dispose();
      if (renderer.domElement.parentNode === container) {
        container.removeChild(renderer.domElement);
      }
    };
  }, []);

  return <div ref={containerRef} className="blob-background" aria-hidden="true" />;
}

export { colorLow, colorHigh, flameColor, flameColor2, atmoColor, bgColor, withAlpha };
