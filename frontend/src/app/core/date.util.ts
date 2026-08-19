export function startOfWeek(date: Date): Date {
  const result = new Date(date);
  const day = result.getDay(); // 0 = dimanche .. 6 = samedi
  const diffToMonday = day === 0 ? -6 : 1 - day;
  result.setDate(result.getDate() + diffToMonday);
  result.setHours(0, 0, 0, 0);
  return result;
}

export function addDays(date: Date, days: number): Date {
  const result = new Date(date);
  result.setDate(result.getDate() + days);
  return result;
}

// yyyy-MM-dd en heure locale, pour la value d'un <input type="date"> : toISOString() décale
// vers l'UTC et peut faire changer de jour selon le fuseau, donc on construit à la main.
export function toDateInputValue(date: Date): string {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

export function combineDateAndTime(dateValue: string, timeValue: string): Date {
  const [year, month, day] = dateValue.split('-').map(Number);
  const [hours, minutes] = timeValue.split(':').map(Number);
  return new Date(year, month - 1, day, hours, minutes, 0, 0);
}
