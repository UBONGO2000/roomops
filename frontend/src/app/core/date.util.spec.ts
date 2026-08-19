import { addDays, combineDateAndTime, startOfWeek, toDateInputValue } from './date.util';

describe('date.util', () => {
  it('startOfWeek returns the Monday of the same week', () => {
    // Jeudi 15 janvier 2026 -> lundi 12 janvier 2026
    const thursday = new Date(2026, 0, 15);
    const monday = startOfWeek(thursday);

    expect(monday.getFullYear()).toBe(2026);
    expect(monday.getMonth()).toBe(0);
    expect(monday.getDate()).toBe(12);
    expect(monday.getHours()).toBe(0);
  });

  it('startOfWeek treats Sunday as the end of the previous week', () => {
    // Dimanche 18 janvier 2026 -> lundi 12 janvier 2026
    const sunday = new Date(2026, 0, 18);
    const monday = startOfWeek(sunday);

    expect(monday.getDate()).toBe(12);
  });

  it('addDays moves forward across a month boundary', () => {
    const jan30 = new Date(2026, 0, 30);
    const result = addDays(jan30, 5);

    expect(result.getMonth()).toBe(1);
    expect(result.getDate()).toBe(4);
  });

  it('toDateInputValue formats using local date parts, not UTC', () => {
    const date = new Date(2026, 0, 5);
    expect(toDateInputValue(date)).toBe('2026-01-05');
  });

  it('combineDateAndTime builds a local Date from separate date/time inputs', () => {
    const result = combineDateAndTime('2030-01-15', '14:30');

    expect(result.getFullYear()).toBe(2030);
    expect(result.getMonth()).toBe(0);
    expect(result.getDate()).toBe(15);
    expect(result.getHours()).toBe(14);
    expect(result.getMinutes()).toBe(30);
  });
});
