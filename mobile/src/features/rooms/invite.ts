import * as Linking from 'expo-linking';

export function inviteLink(code: string): string {
  return Linking.createURL(`join/${code}`);
}

export function inviteMessage(roomName: string, code: string, link: string): string {
  return `Dołącz do pokoju „${roomName}” w Rendezvous: ${link} (kod: ${code})`;
}
