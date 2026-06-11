"use client";

import { useEffect, useMemo, useRef } from "react";
import * as THREE from "three";
import { FavoriteWebtoon } from "@/lib/types";

type Props = {
  favorites: FavoriteWebtoon[];
};

type PlatformRatio = {
  naver: number;
  kakao: number;
  total: number;
};

function resolvePlatformCode(item: FavoriteWebtoon): string {
  if (item.platform?.code) {
    return item.platform.code;
  }
  if (item.originalUrl.includes("kakao")) {
    return "KAKAO_WEBTOON";
  }
  if (item.originalUrl.includes("comic.naver")) {
    return "NAVER_WEBTOON";
  }
  return "UNKNOWN";
}

function calculateRatio(favorites: FavoriteWebtoon[]): PlatformRatio {
  const counts = favorites.reduce(
    (result, item) => {
      const platformCode = resolvePlatformCode(item);
      if (platformCode === "NAVER_WEBTOON") {
        result.naver += 1;
      }
      if (platformCode === "KAKAO_WEBTOON") {
        result.kakao += 1;
      }
      return result;
    },
    { naver: 0, kakao: 0 }
  );

  return {
    ...counts,
    total: counts.naver + counts.kakao
  };
}

function createLiquidMaterial(color: string, boundary: number, side: "left" | "right", visible: boolean) {
  return new THREE.ShaderMaterial({
    uniforms: {
      uColor: { value: new THREE.Color(color) },
      uBoundary: { value: boundary },
      uSide: { value: side === "left" ? 0 : 1 },
      uVisible: { value: visible ? 1 : 0 },
      uTime: { value: 0 }
    },
    vertexShader: `
      varying vec2 vUv;
      void main() {
        vUv = uv;
        gl_Position = projectionMatrix * modelViewMatrix * vec4(position, 1.0);
      }
    `,
    fragmentShader: `
      uniform vec3 uColor;
      uniform float uBoundary;
      uniform float uSide;
      uniform float uVisible;
      uniform float uTime;
      varying vec2 vUv;

      void main() {
        if (uVisible < 0.5) {
          discard;
        }

        vec2 p = vUv * 2.0 - 1.0;
        float circle = dot(p, p);
        if (circle > 0.96) {
          discard;
        }

        float wave = sin(p.y * 5.4 + uTime * 1.35) * 0.026
          + sin(p.y * 10.2 - uTime * 1.7) * 0.012;
        float boundary = clamp(uBoundary + wave, 0.0, 1.0);
        float x = vUv.x;
        if ((uSide < 0.5 && x > boundary) || (uSide > 0.5 && x < boundary)) {
          discard;
        }

        float edgeAlpha = 1.0 - smoothstep(0.84, 0.99, circle);
        float centerGlow = 1.0 - smoothstep(0.0, 0.92, circle);
        float edgeShade = smoothstep(0.52, 0.96, circle);
        float seamGlow = 1.0 - smoothstep(0.0, 0.035, abs(x - boundary));
        vec3 litColor = mix(uColor * 0.68, uColor * 1.16, centerGlow * 0.45 + 0.36);
        litColor = mix(litColor, litColor * 0.72, edgeShade * 0.46);
        litColor += vec3(1.0) * seamGlow * 0.12;
        gl_FragColor = vec4(litColor, 0.92 * edgeAlpha);
      }
    `,
    transparent: true,
    depthTest: false,
    depthWrite: false,
    blending: THREE.NormalBlending,
    side: THREE.DoubleSide
  });
}

export function PlatformRatioOrb({ favorites }: Props) {
  const canvasRef = useRef<HTMLCanvasElement | null>(null);
  const ratio = useMemo(() => calculateRatio(favorites), [favorites]);
  const naverRatio = ratio.total === 0 ? 0 : ratio.naver / ratio.total;
  const kakaoRatio = ratio.total === 0 ? 0 : ratio.kakao / ratio.total;
  const boundaryRatio = Math.min(Math.max(naverRatio, 0), 1);

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) {
      return;
    }
    const container = canvas.parentElement;
    if (!(container instanceof HTMLElement)) {
      return;
    }
    const containerElement: HTMLElement = container;

    const renderer = new THREE.WebGLRenderer({ canvas, alpha: true, antialias: true, preserveDrawingBuffer: true });
    renderer.setClearColor(0x000000, 0);
    renderer.setPixelRatio(Math.min(window.devicePixelRatio || 1, 2));
    renderer.outputColorSpace = THREE.SRGBColorSpace;

    const scene = new THREE.Scene();
    const camera = new THREE.PerspectiveCamera(35, 1, 0.1, 100);
    camera.position.set(0, 0, 5);

    const ambientLight = new THREE.AmbientLight(0xffffff, 1.8);
    const keyLight = new THREE.DirectionalLight(0xffffff, 2.2);
    keyLight.position.set(-3, 4, 5);
    const fillLight = new THREE.DirectionalLight(0xa7d7ff, 1.1);
    fillLight.position.set(4, -2, 4);
    scene.add(ambientLight, keyLight, fillLight);

    const orbGroup = new THREE.Group();
    scene.add(orbGroup);

    const glassGeometry = new THREE.SphereGeometry(1.22, 72, 72);
    const glass = new THREE.Mesh(
      glassGeometry,
      new THREE.MeshPhysicalMaterial({
        color: 0xf8fdff,
        roughness: 0,
        metalness: 0,
        transmission: 0.92,
        thickness: 1.15,
        ior: 1.5,
        reflectivity: 0.76,
        transparent: true,
        opacity: 0.2,
        clearcoat: 1,
        clearcoatRoughness: 0.02
      })
    );
    glass.renderOrder = 5;
    glass.material.depthWrite = false;
    orbGroup.add(glass);

    const innerGlass = new THREE.Mesh(
      new THREE.SphereGeometry(1.12, 64, 64),
      new THREE.MeshBasicMaterial({ color: 0xdff5ff, transparent: true, opacity: 0.1, side: THREE.BackSide })
    );
    innerGlass.renderOrder = 4;
    orbGroup.add(innerGlass);

    const liquidPlane = new THREE.PlaneGeometry(2.18, 2.18, 96, 96);
    const naverMaterial = createLiquidMaterial("#00d564", boundaryRatio, "left", naverRatio > 0);
    const kakaoMaterial = createLiquidMaterial("#fee500", boundaryRatio, "right", kakaoRatio > 0);
    const naverLiquid = new THREE.Mesh(liquidPlane, naverMaterial);
    const kakaoLiquid = new THREE.Mesh(liquidPlane.clone(), kakaoMaterial);
    naverLiquid.position.z = 0.36;
    kakaoLiquid.position.z = 0.38;
    naverLiquid.renderOrder = 1;
    kakaoLiquid.renderOrder = 2;
    orbGroup.add(naverLiquid, kakaoLiquid);

    const backRimMaterial = new THREE.MeshBasicMaterial({ color: 0x9edcff, transparent: true, opacity: 0.13 });
    const backRim = new THREE.Mesh(new THREE.TorusGeometry(1.17, 0.018, 18, 128), backRimMaterial);
    backRim.rotation.x = Math.PI / 2.25;
    backRim.position.z = -0.12;
    orbGroup.add(backRim);

    const rimMaterial = new THREE.MeshBasicMaterial({ color: 0xffffff, transparent: true, opacity: 0.54 });
    const verticalRimMaterial = rimMaterial.clone();
    const horizontalRim = new THREE.Mesh(new THREE.TorusGeometry(1.22, 0.014, 16, 128), rimMaterial);
    horizontalRim.rotation.x = Math.PI / 2.7;
    horizontalRim.renderOrder = 6;
    const verticalRim = new THREE.Mesh(new THREE.TorusGeometry(1.22, 0.011, 16, 128), verticalRimMaterial);
    verticalRim.rotation.y = Math.PI / 2.7;
    verticalRim.rotation.z = Math.PI / 10;
    verticalRim.renderOrder = 6;
    orbGroup.add(horizontalRim, verticalRim);

    const shineMaterial = new THREE.MeshBasicMaterial({ color: 0xffffff, transparent: true, opacity: 0.42 });
    const shine = new THREE.Mesh(new THREE.TorusGeometry(0.42, 0.008, 12, 80), shineMaterial);
    shine.scale.set(1, 0.34, 1);
    shine.rotation.z = -0.55;
    shine.position.set(-0.42, 0.58, 0.98);
    shine.renderOrder = 7;
    orbGroup.add(shine);

    const highlight = new THREE.Mesh(
      new THREE.SphereGeometry(0.17, 24, 24),
      new THREE.MeshBasicMaterial({ color: 0xffffff, transparent: true, opacity: 0.78 })
    );
    highlight.position.set(-0.46, 0.48, 0.9);
    const smallHighlight = highlight.clone();
    smallHighlight.scale.setScalar(0.46);
    smallHighlight.position.set(-0.22, 0.74, 0.86);
    orbGroup.add(highlight, smallHighlight);

    function resize() {
      const rect = containerElement.getBoundingClientRect();
      const width = Math.max(Math.floor(rect.width), 180);
      const height = Math.max(Math.floor(rect.height), 180);
      renderer.setSize(width, height, false);
      camera.aspect = width / height;
      camera.updateProjectionMatrix();
    }

    const resizeObserver = new ResizeObserver(resize);
    resizeObserver.observe(containerElement);
    resize();

    let frameId = 0;
    const clock = new THREE.Clock();

    function animate() {
      const time = clock.getElapsedTime();
      naverMaterial.uniforms.uTime.value = time;
      kakaoMaterial.uniforms.uTime.value = time + 0.8;
      orbGroup.rotation.y = Math.sin(time * 0.45) * 0.1;
      orbGroup.rotation.x = Math.sin(time * 0.38) * 0.035;
      renderer.render(scene, camera);
      frameId = window.requestAnimationFrame(animate);
    }

    animate();

    return () => {
      window.cancelAnimationFrame(frameId);
      resizeObserver.disconnect();
      naverLiquid.geometry.dispose();
      kakaoLiquid.geometry.dispose();
      naverMaterial.dispose();
      kakaoMaterial.dispose();
      glass.geometry.dispose();
      (glass.material as THREE.Material).dispose();
      innerGlass.geometry.dispose();
      (innerGlass.material as THREE.Material).dispose();
      backRim.geometry.dispose();
      backRimMaterial.dispose();
      horizontalRim.geometry.dispose();
      verticalRim.geometry.dispose();
      rimMaterial.dispose();
      verticalRimMaterial.dispose();
      shine.geometry.dispose();
      shineMaterial.dispose();
      highlight.geometry.dispose();
      (highlight.material as THREE.Material).dispose();
      renderer.dispose();
    };
  }, [boundaryRatio, kakaoRatio, naverRatio]);

  return (
    <div className="platform-orb" aria-label={`즐겨찾기 플랫폼 비율: 네이버 ${ratio.naver}개, 카카오 ${ratio.kakao}개`}>
      <div className="platform-orb-canvas">
        <canvas ref={canvasRef} />
      </div>
    </div>
  );
}
