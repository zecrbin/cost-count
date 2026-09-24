/**
 * 吉祥物：小猪存钱罐。mood 控制表情：happy 眯眼笑、calm 睁眼、sad 皱眉、sleepy 闭眼。
 */
export function Mascot({ size = 96, mood = 'calm', coin = false, className = '', style }) {
  const eyes = {
    happy: (
      <g stroke="#3b2a4a" strokeWidth="3.2" strokeLinecap="round" fill="none">
        <path d="M38 50 Q43 44 48 50" />
        <path d="M72 50 Q77 44 82 50" />
      </g>
    ),
    calm: (
      <g>
        <circle cx="43" cy="49" r="4.6" fill="#3b2a4a" />
        <circle cx="77" cy="49" r="4.6" fill="#3b2a4a" />
        <circle cx="44.6" cy="47.3" r="1.6" fill="#fff" />
        <circle cx="78.6" cy="47.3" r="1.6" fill="#fff" />
      </g>
    ),
    sad: (
      <g>
        <circle cx="43" cy="51" r="4.2" fill="#3b2a4a" />
        <circle cx="77" cy="51" r="4.2" fill="#3b2a4a" />
        <g stroke="#3b2a4a" strokeWidth="2.6" strokeLinecap="round">
          <path d="M36 42 L47 45" />
          <path d="M84 42 L73 45" />
        </g>
      </g>
    ),
    sleepy: (
      <g stroke="#3b2a4a" strokeWidth="3.2" strokeLinecap="round" fill="none">
        <path d="M37 50 Q43 55 49 50" />
        <path d="M71 50 Q77 55 83 50" />
      </g>
    ),
  }[mood];

  return (
    <svg viewBox="0 0 120 112" width={size} height={size * (112 / 120)} className={className} style={style} aria-hidden="true">
      {coin && (
        <g className="cc-float" style={{ transformOrigin: '60px 12px' }}>
          <circle cx="60" cy="12" r="10" fill="#ffc94d" stroke="#f0a92b" strokeWidth="2.5" />
          <text x="60" y="16.5" textAnchor="middle" fontSize="12" fontWeight="900" fill="#c07a0c" fontFamily="Arial">¥</text>
        </g>
      )}
      {/* 尾巴 */}
      <path d="M101 62 q10 -4 7 -12 q-3 -6 -8 -1 q-4 5 3 7" fill="none" stroke="#ff8fb2" strokeWidth="3.2" strokeLinecap="round" />
      {/* 耳朵 */}
      <path d="M30 38 L27 17 Q27 13 31 15 L48 28 Z" fill="#ff8fb2" />
      <path d="M90 38 L93 17 Q93 13 89 15 L72 28 Z" fill="#ff8fb2" />
      {/* 腿 */}
      <rect x="33" y="84" width="14" height="16" rx="6" fill="#ff8fb2" />
      <rect x="73" y="84" width="14" height="16" rx="6" fill="#ff8fb2" />
      {/* 身体 */}
      <ellipse cx="60" cy="60" rx="44" ry="36" fill="#ffb3cc" />
      <ellipse cx="46" cy="40" rx="16" ry="8" fill="#ffffff" opacity="0.35" transform="rotate(-18 46 40)" />
      {/* 投币口 */}
      <rect x="49" y="27" width="22" height="5.5" rx="2.75" fill="#e2789c" />
      {eyes}
      {/* 腮红 */}
      <ellipse cx="33" cy="63" rx="7" ry="4" fill="#ff7fa8" opacity="0.45" />
      <ellipse cx="87" cy="63" rx="7" ry="4" fill="#ff7fa8" opacity="0.45" />
      {/* 鼻子 */}
      <ellipse cx="60" cy="65" rx="14" ry="10.5" fill="#ff8fb2" />
      <ellipse cx="55" cy="65" rx="2.6" ry="3.4" fill="#c4527a" />
      <ellipse cx="65" cy="65" rx="2.6" ry="3.4" fill="#c4527a" />
      {/* 嘴 */}
      {mood === 'sad'
        ? <path d="M54 83 Q60 78 66 83" fill="none" stroke="#c4527a" strokeWidth="2.4" strokeLinecap="round" />
        : <path d="M54 79 Q60 84 66 79" fill="none" stroke="#c4527a" strokeWidth="2.4" strokeLinecap="round" />}
    </svg>
  );
}
