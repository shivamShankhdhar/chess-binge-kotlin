'use client';

import { useState, useEffect } from 'react';

interface TypingAnimationProps {
  phrases: string[];
  speed?: number;
  delay?: number;
  className?: string;
}

export default function TypingAnimation({
  phrases,
  speed = 100,
  delay = 2000,
  className = '',
}: TypingAnimationProps) {
  const [displayText, setDisplayText] = useState('');
  const [phraseIndex, setPhraseIndex] = useState(0);
  const [charIndex, setCharIndex] = useState(0);
  const [isDeleting, setIsDeleting] = useState(false);

  useEffect(() => {
    const currentPhrase = phrases[phraseIndex];
    let timeout: NodeJS.Timeout;

    if (!isDeleting) {
      if (charIndex < currentPhrase.length) {
        timeout = setTimeout(() => {
          setDisplayText(currentPhrase.substring(0, charIndex + 1));
          setCharIndex(charIndex + 1);
        }, speed);
      } else {
        timeout = setTimeout(() => {
          setIsDeleting(true);
        }, delay);
      }
    } else {
      if (charIndex > 0) {
        timeout = setTimeout(() => {
          setDisplayText(currentPhrase.substring(0, charIndex - 1));
          setCharIndex(charIndex - 1);
        }, speed / 2);
      } else {
        setIsDeleting(false);
        setPhraseIndex((phraseIndex + 1) % phrases.length);
      }
    }

    return () => clearTimeout(timeout);
  }, [charIndex, isDeleting, phraseIndex, phrases, speed, delay]);

  return (
    <span className={className}>
      {displayText}
      <span className="animate-blink ml-1 inline-block h-5 w-0.5 bg-gradient-to-r from-orange-500 to-orange-600"></span>
    </span>
  );
}
