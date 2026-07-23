import { Injectable, NotFoundException } from '@nestjs/common';
import { randomUUID } from 'crypto';
import { CreatePaymentDto } from './dto/create-payment.dto';

export type PaymentStatus = 'AUTHORIZED' | 'REFUSED';

export interface Payment {
  id: string;
  orderId: string;
  amount: number;
  status: PaymentStatus;
  createdAt: string;
}

@Injectable()
export class PaymentsService {
  private readonly payments = new Map<string, Payment>();

  create(dto: CreatePaymentDto): Payment {
    // Règle métier simple pour démontrer les 2 chemins :
    // au-dessus de 100, le paiement est refusé.
    const status: PaymentStatus = dto.amount > 100 ? 'REFUSED' : 'AUTHORIZED';

    const payment: Payment = {
      id: randomUUID(),
      orderId: dto.orderId,
      amount: dto.amount,
      status,
      createdAt: new Date().toISOString(),
    };
    this.payments.set(payment.orderId, payment);
    return payment;
  }

  statusByOrder(orderId: string): { orderId: string; status: PaymentStatus } {
    const payment = this.payments.get(orderId);
    if (!payment) {
      throw new NotFoundException(`No payment for order ${orderId}`);
    }
    return { orderId, status: payment.status };
  }
}
