"use client";

import { type CSSProperties } from "react";

type Props = {
  seed: string;
  palette: string;
  label: string;
  size?: "sm" | "md" | "lg";
};

const PALETTES: Record<string, { base: string; glow: string; accent: string; ink: string }> = {
  MINT: { base: "#dffbed", glow: "#83e7b1", accent: "#0eb766", ink: "#123428" },
  SUNNY: { base: "#fff4ba", glow: "#ffd34f", accent: "#f09d00", ink: "#3d2c0b" },
  BERRY: { base: "#ffe0ec", glow: "#ff8fbd", accent: "#d63b78", ink: "#3b1024" },
  AURORA: { base: "#e6f6ff", glow: "#93defa", accent: "#4077e6", ink: "#132a4a" },
  SKY: { base: "#e8f1ff", glow: "#9fc2ff", accent: "#3979d8", ink: "#152d4e" },
  LIME: { base: "#efffbd", glow: "#c8ed4f", accent: "#6aa915", ink: "#24350c" },
  CORAL: { base: "#ffe7dc", glow: "#ffa47b", accent: "#e56b3f", ink: "#3d1d11" },
  LILAC: { base: "#efe6ff", glow: "#c5a4ff", accent: "#8059d8", ink: "#28184e" }
};

function hashSeed(seed: string): number {
  let hash = 2166136261;
  for (let index = 0; index < seed.length; index += 1) {
    hash ^= seed.charCodeAt(index);
    hash = Math.imul(hash, 16777619);
  }
  return hash >>> 0;
}

function pickInitial(label: string): string {
  const trimmed = label.trim();
  return trimmed ? trimmed.slice(0, 1) : "W";
}

export function ProfileAvatar({ seed, palette, label, size = "md" }: Props) {
  const avatarPalette = PALETTES[palette] ?? PALETTES.MINT;
  const hash = hashSeed(seed || label || "webtoon-hub");
  const shapeA = 18 + (hash % 34);
  const shapeB = 12 + ((hash >> 5) % 38);
  const rotate = -18 + ((hash >> 10) % 37);
  const initial = pickInitial(label);

  return (
    <div
      className={`profile-avatar profile-avatar-${size}`}
      aria-label={`${label} 프로필 이미지`}
      style={
        {
          "--avatar-base": avatarPalette.base,
          "--avatar-glow": avatarPalette.glow,
          "--avatar-accent": avatarPalette.accent,
          "--avatar-ink": avatarPalette.ink,
          "--avatar-shape-a": `${shapeA}%`,
          "--avatar-shape-b": `${shapeB}%`,
          "--avatar-rotate": `${rotate}deg`
        } as CSSProperties
      }
      role="img"
    >
      <span className="profile-avatar-shape primary" aria-hidden="true" />
      <span className="profile-avatar-shape secondary" aria-hidden="true" />
      <strong>{initial}</strong>
    </div>
  );
}
